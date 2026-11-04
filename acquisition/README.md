# NATURIA Audio Acquisition System

A multi-source pipeline that discovers real nature recordings, verifies the
licence of each individual file, transcodes a compressed streaming version and
publishes the result to our own CDN folder together with a licence register.

```
catalog_spec.py    155 A–Z search intents (what the app wants to offer)
providers.py       Wikimedia Commons, Internet Archive, Freesound, Pixabay, Openverse
license_policy.py  the licence gate (CC0 / PD / CC BY / CC BY-SA / GFDL only)
relevance.py       subject + anti-music filter (a "wind" slot must not get a ragtime tune)
acquire.py         orchestrator: discover → verify → download → transcode → hash → catalogue
```

## Run

```bash
pip install imageio-ffmpeg
export FFMPEG_BIN=$(python -c "import imageio_ffmpeg;print(imageio_ffmpeg.get_ffmpeg_exe())")
export FFPROBE_BIN=$FFMPEG_BIN
python -m acquisition.acquire --start 0 --end 155 --workers 6
```

Outputs:

| File | Contents |
|---|---|
| `catalog/catalog.json` | the catalogue the app ships (bundled in `app/src/main/assets/`) |
| `catalog/licenses.csv` | per-file licence register (creator, licence, source URL, SHA-256) |
| `catalog/ATTRIBUTION.md` | human-readable attribution table |
| `catalog/rejected.json` | every slot that was skipped, with the reason |
| `cdn/audio/<id>.mp3` | 96 kbps mono, loudness-levelled, faded 60 s loops |

## Licence gate

`license_policy.classify()` accepts **CC0, Public Domain Mark, CC BY, CC BY-SA,
GFDL**. It rejects anything NonCommercial, NoDerivatives, Sampling+, "all rights
reserved" or unparseable. NoDerivatives is rejected deliberately: we transcode
every file, which is a derivative work.

## Providers

| Provider | Status in this build | Notes |
|---|---|---|
| Wikimedia Commons | **live, 121 files** | API, no key, per-file `extmetadata` licence |
| Internet Archive | **live, 6 files** | metadata API + explicit `licenseurl` only |
| Freesound | adapter ready | needs `FREESOUND_API_KEY`; per-sound licence mapped (CC0/BY/BY-SA) |
| Pixabay | adapter ready | public API exposes images/video only; no audio endpoint exists |
| Openverse | adapter ready | needs `OPENVERSE_TOKEN` and egress to `api.openverse.org` (blocked in this environment) |

Nothing is downloaded before it passes the licence gate, so an adapter can be
switched on at any time without risking a non-permitted file entering the build.
