#!/usr/bin/env python3
"""NATURIA acquisition pipeline.

discover -> verify licence -> download -> transcode (compressed loop) ->
hash -> store on own CDN folder -> write catalogue + attribution database.

Usage:
  python -m acquisition.acquire --start 0 --end 40
  python -m acquisition.acquire --report
"""
import argparse
import csv
import hashlib
import json
import os
import re
import subprocess
import sys
import threading
from concurrent.futures import ThreadPoolExecutor

from . import providers, relevance
from .catalog_spec import SPEC
from .license_policy import classify

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW = os.path.join(ROOT, "cdn", "raw")
AUDIO = os.path.join(ROOT, "cdn", "audio")
CAT = os.path.join(ROOT, "catalog")
STATE = os.path.join(CAT, "state.json")
LOCK = threading.RLock()

FFMPEG = os.environ.get("FFMPEG_BIN") or "ffmpeg"
FFPROBE = os.environ.get("FFPROBE_BIN") or "ffprobe"
LOOP_SECONDS = 60
BITRATE = "96k"

for d in (RAW, AUDIO, CAT):
    os.makedirs(d, exist_ok=True)


def slug(name):
    return re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")


def ffprobe_duration(path):
    """Duration in seconds. Uses ffprobe when present, otherwise parses ffmpeg -i."""
    try:
        r = subprocess.run([FFPROBE, "-v", "error", "-show_entries", "format=duration",
                            "-of", "default=nw=1:nk=1", path],
                           capture_output=True, text=True, timeout=60)
        out = (r.stdout or "").strip()
        if r.returncode == 0 and out:
            return float(out)
    except Exception:
        pass
    try:
        r = subprocess.run([FFMPEG, "-hide_banner", "-i", path],
                           capture_output=True, text=True, timeout=60)
        m = re.search(r"Duration:\s*(\d+):(\d+):(\d+\.?\d*)", r.stderr or "")
        if m:
            h, mi, se = m.groups()
            return int(h) * 3600 + int(mi) * 60 + float(se)
    except Exception:
        pass
    return 0.0


def transcode(src, dst):
    """Compressed, loop-friendly mono MP3 with fades and loudness levelling."""
    dur = ffprobe_duration(src)
    if dur < 20:
        return None, dur
    take = min(LOOP_SECONDS, dur)
    start = 0.0 if dur <= LOOP_SECONDS else min(dur * 0.15, dur - take)
    fade = min(2.0, take / 8)
    af = (f"loudnorm=I=-18:TP=-2:LRA=11,"
          f"afade=t=in:st=0:d={fade:.2f},afade=t=out:st={take - fade:.2f}:d={fade:.2f}")
    cmd = [FFMPEG, "-y", "-v", "error", "-ss", f"{start:.2f}", "-t", f"{take:.2f}", "-i", src,
           "-ac", "1", "-ar", "44100", "-b:a", BITRATE, "-af", af, "-map_metadata", "-1", dst]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=180)
    if r.returncode != 0 or not os.path.exists(dst) or os.path.getsize(dst) < 20000:
        return None, dur
    return take, dur


def score(cand, entry):
    name, letter, group, queries, tags = entry
    s = relevance.relevance(name, group, tags, cand.get("title"))
    mime = (cand.get("mime") or "").lower()
    if "ogg" in mime or "opus" in mime:
        s += 8
    elif "mpeg" in mime or "mp3" in mime:
        s += 7
    sz = cand.get("size") or 0
    if 200_000 <= sz <= 6_000_000:
        s += 6
    elif sz > 20_000_000:
        s -= 12
    lic = (cand.get("license_name") or "").lower()
    if "cc0" in lic or "public domain" in lic:
        s += 6
    if cand.get("provider") == "wikimedia_commons":
        s += 4
    d = cand.get("duration")
    try:
        d = float(d)
        if 25 <= d <= 600:
            s += 5
        elif d < 12:
            s -= 10
    except (TypeError, ValueError):
        pass
    return s


def load_state():
    if os.path.exists(STATE):
        with open(STATE) as f:
            return json.load(f)
    return {"done": {}, "rejected": [], "used_urls": [], "used_hashes": [], "cache": {}}


def save_state(st):
    with LOCK:
        blob = json.dumps(st, indent=1)
    tmp = STATE + ".tmp"
    with open(tmp, "w") as f:
        f.write(blob)
    os.replace(tmp, STATE)


GENERIC = {
    "Wind": ["wind ambience recording", "wind sound effect", "blizzard sound", "gale wind sound"],
    "Rain": ["rain sound effect", "rainfall recording", "rain ambience"],
    "Water": ["water sound effect", "river sound effect", "stream recording"],
    "Ocean": ["sea sound recording", "waves sound effect", "shore ambience"],
    "Fire": ["fire crackling recording", "fireplace sound effect"],
    "Storm": ["thunder sound effect", "storm recording", "thunderstorm recording"],
    "Night": ["night ambience recording", "crickets recording", "night soundscape"],
    "Forest": ["forest ambience recording", "woodland sound", "forest sound effect"],
    "Birds": ["birds sound effect", "birdsong recording", "bird calls recording"],
    "Wildlife": ["animal sound effect", "wildlife recording", "animal calls recording"],
    "Insects": ["insect sound effect", "crickets recording", "cicada recording"],
    "Ambience": ["nature ambience recording", "field recording nature", "nature soundscape"],
}


def _queries(entry):
    name, letter, group, queries, tags = entry
    qs = list(queries)
    for extra in (f"{name} sound", f"{name} ambience", f"{name} recording"):
        if extra not in qs:
            qs.append(extra)
    for g in GENERIC.get(group, []):
        if g not in qs:
            qs.append(g)
    return qs


def _cached_search(st, prov, q, limit):
    key = f"{prov}|{q}|{limit}"
    if key in st["cache"]:
        return st["cache"][key]
    try:
        res = providers.PROVIDERS[prov](q, limit)
    except Exception:
        res = []
    with LOCK:
        st["cache"][key] = res
    return res


def process(entry, st):
    name, letter, group, queries, tags = entry
    key = slug(name)
    if key in st["done"]:
        return "cached"
    tried = []
    pool = []
    provs = ("wikimedia_commons",) if os.environ.get("NATURIA_FAST") else ("wikimedia_commons", "internet_archive")
    for prov_name in provs:
        qs = _queries(entry) if prov_name == "wikimedia_commons" else queries[:2]
        for q in qs:
            for c in _cached_search(st, prov_name, q, 40 if "recording" in q or "effect" in q else 20):
                if not c.get("file_url") or c["file_url"] in st["used_urls"]:
                    continue
                if not (60_000 < (c.get("size") or 0) < 30_000_000):
                    continue
                if not relevance.subject_ok(name, group, tags, c.get("title"), c.get("description")):
                    tried.append(f"irrelevant:{c.get('title','')[:45]}")
                    continue
                info = classify(c.get("license_name"), c.get("license_url"))
                if info is None:
                    tried.append(f"licence:{c.get('license_name')}:{c.get('title','')[:35]}")
                    continue
                c["license_code"], c["license_human"] = info[0], info[1]
                c["attribution_required"] = info[2]
                pool.append(c)
    if not pool:
        with LOCK:
            st["rejected"].append({"name": name, "letter": letter, "tried": tried[-6:]})
            save_state(st)
        return "rejected"

    seen = set()
    uniq = []
    for c in pool:
        if c["file_url"] in seen:
            continue
        seen.add(c["file_url"])
        uniq.append(c)
    uniq.sort(key=lambda c: score(c, entry), reverse=True)

    for c in uniq[:3]:
        raw_path = os.path.join(RAW, f"{key}.src")
        try:
            with open(raw_path, "wb") as f:
                f.write(providers._get(c["file_url"], timeout=45))
        except Exception as e:
            tried.append(f"download:{c.get('title','')[:30]}:{e}")
            continue
        out = os.path.join(AUDIO, f"{key}.mp3")
        take, orig = transcode(raw_path, out)
        if not take:
            tried.append(f"transcode:{c.get('title','')[:30]}")
            try:
                os.remove(raw_path)
            except OSError:
                pass
            continue
        h = hashlib.sha256(open(out, "rb").read()).hexdigest()
        if h in st["used_hashes"]:
            tried.append(f"duplicate:{c.get('title','')[:30]}")
            continue
        st["used_hashes"].append(h)
        st["used_urls"].append(c["file_url"])
        rec = {
            "id": key, "name": name, "letter": letter, "group": group, "tags": tags,
            "provider": c["provider"], "provider_title": c.get("title"),
            "source_page": c.get("page_url"), "original_file": c.get("file_url"),
            "creator": (c.get("creator") or "").strip() or "Unknown",
            "license": c["license_human"], "license_code": c["license_code"],
            "license_url": c.get("license_url") or "",
            "attribution_required": c["attribution_required"],
            "duration": round(take, 1), "original_duration": round(orig, 1),
            "size": os.path.getsize(out), "sha256": h,
            "cdn_path": f"cdn/audio/{key}.mp3",
            "bitrate": BITRATE, "channels": 1, "sample_rate": 44100,
            "description": (c.get("description") or "")[:300],
        }
        with LOCK:
            st["done"][key] = rec
            save_state(st)
        try:
            os.remove(raw_path)
        except OSError:
            pass
        return "ok"
    with LOCK:
        st["rejected"].append({"name": name, "letter": letter, "tried": tried[-6:]})
        save_state(st)
    return "rejected"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--start", type=int, default=0)
    ap.add_argument("--end", type=int, default=len(SPEC))
    ap.add_argument("--workers", type=int, default=4)
    ap.add_argument("--report", action="store_true")
    a = ap.parse_args()

    st = load_state()
    if a.report:
        write_outputs(st)
        return

    todo = [e for e in SPEC[a.start:a.end] if slug(e[0]) not in st["done"]]
    print(f"processing {len(todo)} entries (workers={a.workers})", flush=True)
    done = 0
    with ThreadPoolExecutor(max_workers=a.workers) as ex:
        for res in ex.map(lambda e: process(e, st), todo):
            done += 1
            if done % 10 == 0:
                print(f"  {done}/{len(todo)} ok={len(st['done'])} rejected={len(st['rejected'])}", flush=True)
    print(f"FINISHED slice: catalogue={len(st['done'])} rejected={len(st['rejected'])}")
    write_outputs(st)


def write_outputs(st):
    recs = sorted(st["done"].values(), key=lambda r: (r["letter"], r["name"]))
    with open(os.path.join(CAT, "catalog.json"), "w") as f:
        json.dump({"version": 1, "generated": "2026-11-04", "count": len(recs),
                   "sounds": recs}, f, indent=1)
    with open(os.path.join(CAT, "rejected.json"), "w") as f:
        json.dump(st["rejected"], f, indent=1)
    with open(os.path.join(CAT, "licenses.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["id", "name", "letter", "creator", "license", "license_url",
                    "provider", "source_page", "original_file", "cdn_path", "sha256", "duration_s"])
        for r in recs:
            w.writerow([r["id"], r["name"], r["letter"], r["creator"], r["license"], r["license_url"],
                        r["provider"], r["source_page"], r["original_file"], r["cdn_path"],
                        r["sha256"], r["duration"]])
    lines = ["# NATURIA - Attribution & Licence Register", "",
             f"{len(recs)} recordings. Every file below is CC0, public domain, CC BY or CC BY-SA "
             "and is redistributed in compressed form with attribution.", "",
             "| Sound | Creator | Licence | Source |", "|---|---|---|---|"]
    for r in recs:
        lines.append(f"| {r['name']} | {r['creator'][:60]} | {r['license']} | [{r['provider']}]({r['source_page']}) |")
    open(os.path.join(CAT, "ATTRIBUTION.md"), "w").write("\n".join(lines) + "\n")
    print(f"wrote catalog.json ({len(recs)}), licenses.csv, ATTRIBUTION.md, rejected.json")


if __name__ == "__main__":
    sys.exit(main())
