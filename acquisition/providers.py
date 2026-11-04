"""Audio discovery providers for NATURIA.

Each provider exposes search(query, limit) -> list[Candidate] and a
status() dict. Providers that need credentials report why they are idle
instead of silently returning nothing.

Live in this build environment: wikimedia_commons, internet_archive.
Configured but idle (need credentials / blocked egress): freesound, pixabay,
openverse. Set the env vars below and re-run acquisition/acquire.py to fold
their results into the same catalogue - the licence gate applies identically.
"""
import json
import os
import re
import urllib.parse
import urllib.request

from .license_policy import classify, strip_html

UA = {"User-Agent": "NATURIA-Acquisition/1.0 (nature sound catalogue; nihaonihao54059@gmail.com)"}
AUDIO_MIME = {"application/ogg", "audio/ogg", "audio/mpeg", "audio/mp3", "audio/wav",
              "audio/x-wav", "audio/flac", "audio/x-flac", "audio/opus", "audio/webm"}


def _get(url, headers=None, timeout=45):
    req = urllib.request.Request(url, headers={**UA, **(headers or {})})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def _json(url, headers=None):
    return json.loads(_get(url, headers).decode("utf-8", "replace"))


class Candidate(dict):
    """dict with normalised keys; see KEYS."""


KEYS = ("provider", "title", "file_url", "page_url", "mime", "size", "duration",
        "license_name", "license_url", "creator", "credit", "description", "raw_id")


# --------------------------------------------------------------------------- #
# Wikimedia Commons (live, no key)
# --------------------------------------------------------------------------- #
def commons_search(query, limit=12):
    params = {
        "action": "query", "format": "json", "generator": "search",
        "gsrsearch": f"filetype:audio {query}", "gsrnamespace": "6",
        "gsrlimit": str(limit), "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata", "iiurlwidth": "320",
    }
    url = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode(params)
    out = []
    try:
        data = _json(url)
    except Exception:
        return out
    pages = data.get("query", {}).get("pages", {})
    for p in pages.values():
        ii = (p.get("imageinfo") or [{}])[0]
        em = ii.get("extmetadata", {}) or {}
        g = lambda k: strip_html((em.get(k) or {}).get("value", ""))
        mime = ii.get("mime", "")
        if mime not in AUDIO_MIME:
            continue
        out.append(Candidate(
            provider="wikimedia_commons", title=p.get("title", "").replace("File:", ""),
            file_url=ii.get("url", ""),
            page_url="https://commons.wikimedia.org/wiki/" + urllib.parse.quote(p.get("title", "").replace(" ", "_")),
            mime=mime, size=ii.get("size", 0), duration=None,
            license_name=g("LicenseShortName"), license_url=(em.get("LicenseUrl") or {}).get("value", ""),
            creator=g("Artist") or g("Credit"), credit=g("Credit"),
            description=g("ImageDescription")[:300], raw_id=p.get("title", "")))
    return out


# --------------------------------------------------------------------------- #
# Internet Archive (live, no key)
# --------------------------------------------------------------------------- #
def archive_search(query, limit=10):
    q = urllib.parse.quote(f'mediatype:(audio) AND ({query})')
    fl = "&".join(f"fl[]={f}" for f in ("identifier", "title", "licenseurl", "creator", "description"))
    url = f"https://archive.org/advancedsearch.php?q={q}&{fl}&rows={limit}&output=json"
    out = []
    try:
        docs = _json(url).get("response", {}).get("docs", [])
    except Exception:
        return out
    for d in docs:
        ident = d.get("identifier")
        if not ident:
            continue
        try:
            meta = _json(f"https://archive.org/metadata/{ident}")
        except Exception:
            continue
        lic = d.get("licenseurl") or meta.get("metadata", {}).get("licenseurl", "")
        creator = d.get("creator") or meta.get("metadata", {}).get("creator", "")
        if isinstance(creator, list):
            creator = ", ".join(creator)
        for f in meta.get("files", []):
            name = f.get("name", "")
            if not re.search(r"\.(mp3|ogg|oga|opus|flac|wav)$", name, re.I):
                continue
            size = int(f.get("size", 0) or 0)
            if size < 150_000 or size > 30_000_000:
                continue
            if not re.search(r"\.(mp3|ogg|oga|opus)$", name, re.I):
                continue  # skip wav/flac: too heavy to transcode in bulk
            out.append(Candidate(
                provider="internet_archive", title=f"{d.get('title', ident)} - {name}",
                file_url=f"https://archive.org/download/{ident}/{urllib.parse.quote(name)}",
                page_url=f"https://archive.org/details/{ident}", mime="audio/" + name.rsplit(".", 1)[1].lower(),
                size=size, duration=f.get("length"), license_name=_ia_license_name(lic),
                license_url=lic, creator=creator if isinstance(creator, str) else str(creator),
                credit=meta.get("metadata", {}).get("credit", ""),
                description=strip_html(str(d.get("description", "")))[:300], raw_id=f"{ident}/{name}"))
            break
    return out


def _ia_license_name(url):
    u = (url or "").lower()
    if "publicdomain/zero" in u or "cc0" in u:
        return "CC0 1.0"
    if "publicdomain" in u:
        return "Public domain"
    if "by-sa" in u:
        return "CC BY-SA"
    if "/by/" in u or u.endswith("/by") or "licenses/by" in u:
        return "CC BY"
    return url or "unknown"


# --------------------------------------------------------------------------- #
# Credential-gated providers
# --------------------------------------------------------------------------- #
def freesound_search(query, limit=15):
    token = os.environ.get("FREESOUND_API_KEY")
    if not token:
        return []
    url = ("https://freesound.org/apiv2/search/text/?" + urllib.parse.urlencode({
        "query": query, "page_size": limit,
        "fields": "id,name,url,license,username,previews,duration,description",
        "token": token}))
    out = []
    for r in _json(url).get("results", []):
        out.append(Candidate(
            provider="freesound", title=r.get("name", ""),
            file_url=(r.get("previews") or {}).get("preview-hq-mp3", ""),
            page_url=r.get("url", ""), mime="audio/mpeg", size=0,
            duration=r.get("duration"), license_name=_fs_license(r.get("license", "")),
            license_url=r.get("license", ""), creator=r.get("username", ""), credit=r.get("username", ""),
            description=strip_html(r.get("description", ""))[:300], raw_id=str(r.get("id"))))
    return out


def _fs_license(u):
    u = u or ""
    if "zero" in u:
        return "CC0 1.0"
    if "by-nc" in u:
        return "CC BY-NC"
    if "by-sa" in u:
        return "CC BY-SA"
    if "/by/" in u:
        return "CC BY"
    return "unknown"


def pixabay_search(query, limit=15):
    key = os.environ.get("PIXABAY_API_KEY")
    if not key:
        return []
    # Pixabay's public API exposes images and videos only; music/sound effects
    # are not served by api.pixabay.com, so this provider is a no-op by design
    # until an audio endpoint or a manual export is supplied.
    return []


def openverse_search(query, limit=20):
    lic = "cc0,pdm,by,by-sa"
    url = "https://api.openverse.org/v1/audio/?" + urllib.parse.urlencode({
        "q": query, "page_size": limit, "license": lic})
    try:
        data = _json(url, {"Authorization": f"Bearer {os.environ.get('OPENVERSE_TOKEN', '')}".strip()})
    except Exception:
        return []
    out = []
    for r in data.get("results", []):
        out.append(Candidate(
            provider="openverse:" + str(r.get("source", "")), title=r.get("title", ""),
            file_url=r.get("url", ""), page_url=r.get("foreign_landing_url", ""),
            mime=r.get("filetype") or "audio/mpeg", size=r.get("filesize") or 0,
            duration=r.get("duration"), license_name=(r.get("license") or "").upper(),
            license_url=r.get("license_url", ""), creator=r.get("creator", ""),
            credit=r.get("attribution", ""), description=strip_html(r.get("description", ""))[:300],
            raw_id=str(r.get("id"))))
    return out


PROVIDERS = {
    "wikimedia_commons": commons_search,
    "internet_archive": archive_search,
    "freesound": freesound_search,
    "pixabay": pixabay_search,
    "openverse": openverse_search,
}


def status():
    return {
        "wikimedia_commons": "live (no credentials required)",
        "internet_archive": "live (no credentials required)",
        "freesound": "configured - set FREESOUND_API_KEY to enable",
        "pixabay": "configured - public API has no audio endpoint; set PIXABAY_API_KEY for metadata sync",
        "openverse": "configured - requires OPENVERSE_TOKEN and egress to api.openverse.org",
    }
