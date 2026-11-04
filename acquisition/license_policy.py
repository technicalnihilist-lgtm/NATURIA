"""Licence verification for NATURIA.

Policy: a recording may enter the catalogue only if it is CC0 / Public Domain /
CC BY / CC BY-SA / GFDL. Anything NonCommercial, NoDerivatives, Sampling+,
"all rights reserved", unknown or unparseable is REJECTED, because NATURIA
(a) redistributes the file from its own CDN and (b) transcodes it to a
compressed streaming version - both of which are derivative redistribution.
"""
import re

ALLOWED = {
    "cc0": ("CC0 1.0", "Creative Commons Zero - public domain dedication", True, True, True),
    "pdm": ("Public Domain Mark", "Public domain", True, True, True),
    "pd": ("Public Domain", "Public domain", True, True, True),
    "by": ("CC BY", "Creative Commons Attribution", True, True, True),
    "by-sa": ("CC BY-SA", "Creative Commons Attribution-ShareAlike", True, True, True),
    "gfdl": ("GFDL", "GNU Free Documentation License", True, True, True),
}

BLOCK = ("nc", "nd", "sampling", "noncommercial", "noderiv", "all rights reserved",
         "fair use", "non-free", "copyright", "unknown")


def _norm(text):
    return (text or "").strip().lower()


def classify(license_name, license_url=""):
    """Return (code, human_name, attribution_required, redistribute_ok, commercial_ok) or None."""
    n = _norm(license_name)
    u = _norm(license_url)
    blob = n + " " + u

    # explicit blocks first
    if "by-nc" in blob or "by_nc" in blob or "noncommercial" in blob or "-nc-" in blob or blob.endswith("-nc") or " nc" in " " + n:
        return None
    if "by-nd" in blob or "-nd" in blob or "noderiv" in blob or "no derivative" in blob:
        return None
    if "sampling" in blob or "all rights reserved" in blob or "fair use" in blob:
        return None

    if "cc0" in blob or "zero" in blob:
        return ALLOWED["cc0"]
    if "public domain" in blob or "pdm" in blob or "pd-" in blob or blob.strip() in ("pd", "publicdomain"):
        return ALLOWED["pdm"]
    if "by-sa" in blob or "by_sa" in blob or "sharealike" in blob or "share-alike" in blob:
        return ALLOWED["by-sa"]
    if "gfdl" in blob or "fdl" in blob:
        return ALLOWED["gfdl"]
    if re.search(r"\bby\b", blob) or "attribution" in blob:
        return ALLOWED["by"]
    return None


def is_permitted(license_name, license_url=""):
    return classify(license_name, license_url) is not None


def strip_html(s):
    if not s:
        return ""
    s = re.sub(r"<[^>]+>", " ", s)
    s = (s.replace("&amp;", "&").replace("&quot;", '"').replace("&#039;", "'")
          .replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">"))
    return re.sub(r"\s+", " ", s).strip()
