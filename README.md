<div align="center">

<a href="https://github.com/technicalnihilist-lgtm/NATURIA/stargazers"><img src="https://img.shields.io/badge/stars-0-5A31F4?style=for-the-badge&logo=github&logoColor=white" alt="stars: 0"></a>&nbsp;<a href="https://github.com/technicalnihilist-lgtm/NATURIA/network/members"><img src="https://img.shields.io/badge/forks-0-3FB950?style=for-the-badge" alt="forks: 0"></a>

<h3>NATURIA — premium Android nature sounds app: 127 licence-verified recordings A–Z, 6-layer mixing…</h3>

<img src="https://img.shields.io/badge/Java-5865F2?style=flat-square" alt="Java"> <img src="https://img.shields.io/badge/Zero%20trackers-3B3F4A?style=flat-square" alt="Zero trackers"> <img src="https://img.shields.io/badge/Open%20source-3FB950?style=flat-square" alt="Open source">

<a href="https://github.com/technicalnihilist-lgtm/NATURIA/releases/latest"><img src="https://img.shields.io/badge/DOWNLOAD-APK-6D5AE6?style=for-the-badge&logo=android&logoColor=white" alt="Download APK"></a>

<b>Support the work</b>

<img src="https://img.shields.io/badge/Buy%20me%20a%20coffee-COMING%20SOON-4B5563?style=for-the-badge&logo=buymeacoffee&logoColor=white" alt="Buy me a coffee — coming soon"> <img src="https://img.shields.io/badge/UPI-COMING%20SOON-4B5563?style=for-the-badge&logo=googlepay&logoColor=white" alt="UPI — coming soon"> <img src="https://img.shields.io/badge/PayPal-COMING%20SOON-4B5563?style=for-the-badge&logo=paypal&logoColor=white" alt="PayPal — coming soon">

<sub>Donations aren't live yet — Paytm, UPI, PayPal and Buy&nbsp;Me&nbsp;a&nbsp;Coffee all show <b>coming soon</b>. Until then, a ⭐ star is the best support.</sub>

<a href="https://github.com/technicalnihilist-lgtm/NATURIA/releases/latest"><img src="https://img.shields.io/badge/VERSION-v1.2.0-6D5AE6?style=for-the-badge" alt="VERSION: v1.2.0"></a> <img src="https://img.shields.io/badge/DOWNLOADS-9-3FB950?style=for-the-badge" alt="DOWNLOADS: 9"> <a href="https://github.com/technicalnihilist-lgtm/NATURIA/releases"><img src="https://img.shields.io/badge/RELEASES-GITHUB-EC4899?style=for-the-badge&logo=github&logoColor=white" alt="RELEASES: GITHUB"></a>

<h4>Get it from</h4>

<a href="https://github.com/technicalnihilist-lgtm/NATURIA/releases"><img src="https://img.shields.io/badge/GitHub-Releases-181717?style=for-the-badge&logo=github&logoColor=white" alt="GitHub Releases"></a> <a href="https://github.com/technicalnihilist-lgtm/NATURIA/archive/refs/heads/main.zip"><img src="https://img.shields.io/badge/Source-ZIP-0EA5E9?style=for-the-badge&logo=github&logoColor=white" alt="Source ZIP"></a>

</div>

---

---

## What is inside

| | |
|---|---|
| Recordings | **127**, covering **all 26 letters A–Z**, 11 categories |
| Total audio | 82 MB of 96 kbps mono MP3, ~52 s per loop, loudness-levelled |
| Licences | Public domain 51 · CC BY-SA 41 · CC BY 25 · CC0 10 — **zero** unlicensed files |
| Sources | Wikimedia Commons 94 · Internet Archive 33 |
| APK | 9.1 MB, signed, `minSdk 24`, `targetSdk 34` |

### Features

**Discovery** — search across names, categories, tags and descriptions; A–Z index
with per-letter counts; featured, popular, recently-played rails; a deterministic
"daily nature sound" that is the same for everyone on a given day.

**Player** — full-screen artwork, transport, seek, per-sound volume, favourites,
hand-off into the mixer, a licence sheet with the original source link, and a
licence line under the controls showing creator, licence and provider. Every
sound **loops forever** by default (`REPEAT_MODE_ONE`) and the loop button turns
that on and off for real. Nothing is stored on the device.

**Sleep timer** — 5 / 10 / 15 / 30 / 60 minutes. The last eight seconds fade out
before playback stops, so it never cuts abruptly.

**Mixing studio** — layer up to six sounds, each with its own volume slider;
five one-tap presets (Rain + Thunder, Ocean + Seagulls, Forest + Birds,
River + Wind, Campfire + Night Crickets); save and replay your own mixes.

**Live catalogue** — the app ships a seed catalogue in assets, then pulls
`catalog/catalog.json` from the CDN on every start and swaps it in (cached to
internal storage, with a toast when new sounds arrive). Add a recording to the
CDN and it appears under *New sounds* on the next launch — no APK update.

**Library** — favourites, play history and a *New* tab, all in SQLite.

**Background playback** — a `MediaSessionService` with ExoPlayer: notification,
lock-screen controls, audio-focus handling, becoming-noisy handling, wake mode,
and no battery-hungry polling. In mix mode the layers are kept in step with
layer 0 every 400 ms so a lock-screen pause pauses the whole mix.

**Design** — deep forest green, soft blue and mint; translucent glass cards with
hairline strokes; 28 dp radii; CC-licensed nature photography as category art;
day/night themes (system, light, dark) with a proper light palette.

**Online only** — no download manager, no media stored on the phone, no storage
permission. If the network drops the player says so instead of failing silently.

**Privacy** — no account, no login, no analytics, no ads, no social features.
Permissions: `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`,
`FOREGROUND_SERVICE(+MEDIA_PLAYBACK)`, `POST_NOTIFICATIONS`. Nothing else.

---

## Audio catalogue and acquisition

Nothing in this app is a placeholder. Every file was found by the acquisition
pipeline in [`acquisition/`](acquisition/), licence-checked *before* download,
transcoded, hashed, and stored on our own CDN folder (`cdn/audio/`, served over
GitHub raw). The app streams from that folder and caches the same file locally
when you download.

```
discover ─► licence gate ─► subject filter ─► download ─► transcode ─► SHA-256 ─► catalogue
             (CC0/PD/BY/BY-SA)   (no music/speech)        96k mono 60s loop
```

* **Licence gate** — `CC0`, `Public Domain Mark`, `CC BY`, `CC BY-SA`, `GFDL` are
  accepted. `NC`, `ND`, `Sampling+`, "all rights reserved" and anything
  unparseable are rejected automatically. `ND` is rejected on purpose: we
  transcode every file, and transcoding is a derivative.
* **Subject filter** — the noisy part of open audio search. It rejects music
  (ragtime, waltzes, choir, "Op." numbers), spoken word (Wikipedia articles,
  court opinions, news, podcasts), game and film audio, and Xeno-canto bird
  files that were only matched because the description mentions wind.
* **Register** — `catalog/licenses.csv` and `catalog/ATTRIBUTION.md` carry
  creator, licence, licence URL, source page, original file URL and SHA-256 for
  every recording. The same data is in the app: every player shows the licence
  line, and *Settings → About & licences* lists all 127 entries with tappable
  source links.
* **Skipped slots** — 28 of the 155 search intents had no recording that passed
  both gates, so they were dropped rather than faked. They are recorded with
  their rejection reasons in `catalog/rejected.json`.

Provider status is documented in [`acquisition/README.md`](acquisition/README.md).
In short: Wikimedia Commons and Internet Archive are live and keyless; Freesound
and Pixabay adapters are wired but need API credentials (and Pixabay's public API
has no audio endpoint at all); Openverse needs a token and is not reachable from
this build environment. Because the licence gate sits in front of every provider,
enabling any of them later cannot let a non-permitted file into the build.

---

## Architecture

```
app/src/main/java/com/naturia/app/
├── model/      Sound, Mix, MixLayer
├── data/       CatalogRepository (assets/catalog.json + CDN URLs)
│               AppDb (SQLite: favourites, history, mixes, downloads)
│               DownloadStore (offline cache, progress, atomic .part → rename)
├── playback/   PlaybackHub (shared state) · PlaybackService (MediaSessionService,
│               ExoPlayer single + up to 6 mix players, sleep timer, notifications)
├── ui/         MainActivity · Home · Explore · Mix · Library fragments
│               PlayerActivity · SettingsActivity · AboutActivity · Adapters
└── util/       Art (asset artwork with LruCache)
```

* **Playback engine** — Media3 ExoPlayer, one instance for single playback and
  one per mix layer, all `REPEAT_MODE_ONE` for gapless looping.
* **State** — `PlaybackHub` is a small observable singleton in the same process,
  so the mini-player, player screen and service stay in sync without IPC.
* **Storage** — plain `SQLiteOpenHelper` (no Room, no annotation processing), so
  the build stays fast and the APK small.
* **Network** — `HttpURLConnection` only. No OkHttp, no image library; artwork is
  bundled and decoded from assets into an `LruCache`.
* **Delivery** — audio streams from jsDelivr (`cdn.jsdelivr.net/gh/...@main`), a
  real edge CDN, because `raw.githubusercontent.com` is throttled on many Indian
  networks. If a track fails to load, the player retries once against the GitHub
  raw mirror before showing an error, and a 30 s / 120 s load-control window
  keeps slow first reads from looking like dead tracks.
* **Live catalogue** — `CatalogRepository` parses the bundled seed synchronously,
  then refreshes from the CDN on a background thread and notifies the UI. The
  last good payload is cached in internal storage, so a cold start with no
  network still shows the full catalogue and streams as soon as it reconnects.
* **Failure handling** — a failed refresh keeps the previous catalogue and marks
  the header *Offline*; playback errors surface as a toast rather than a crash.

### Build it yourself

```bash
## Honest notes

* The screenshots above are renders of the app's design system at 360 × 780 dp
  built from the real catalogue data and the bundled artwork — this build
  environment has no KVM, so no emulator was available for device captures.
* The APK compiles and is signed and verified (`apksigner verify` passes), but it
  has not been exercised on a physical device here.
* v1.1 is signed with a new key, so uninstall v1.0 before installing it.
* Some sourced recordings are close matches rather than perfect ones (a "Valley
  Wind" slot may be filled by a generic wind field recording). Titles, creators
  and sources are always the real ones, and every file is genuinely the type of
  sound it is filed under.

## Licence

App code: MIT. Audio: each recording keeps its own licence, listed in
[`catalog/ATTRIBUTION.md`](catalog/ATTRIBUTION.md) — mostly public domain and
CC BY-SA, which require attribution (shown in-app) and share-alike for the
audio itself. Artwork: CC-licensed photography from Wikimedia Commons.

---

## 🌍 Languages

<sub>Donations are <b>coming soon</b> in every language below — nothing to pay today, nothing to click.</sub>

<div align="center">

<sub><b>हिन्दी · বাংলা · मराठी · తెలుగు · தமிழ் · ગુજરાતી · ಕನ್ನಡ · മലയാളം · ਪੰਜਾਬੀ · ଓଡ଼ିଆ · অসমীয়া · اردو</b></sub>

</div>

<details>
<summary><b>🌍 Open the same message in 53 languages</b></summary>

**🇮🇳 Indian languages**

- **हिन्दी** (Hindi) — ☕ जल्द ही आ रहा है। · ⭐ अगर आपको यह ऐप और हमारा काम पसंद है, तो कृपया रिपॉजिटरी को स्टार दें और हमें फॉलो करें।
- **বাংলা** (Bengali) — ☕ খুব শিগগিরই আসছে। · ⭐ যদি আপনার এই অ্যাপ এবং আমাদের কাজ ভালো লাগে, তবে অনুগ্রহ করে রিপোজিটরিটিকে একটি স্টার দিন এবং আমাদের ফলো করুন।
- **मराठी** (Marathi) — ☕ लवकरच येत आहे. · ⭐ जर तुम्हाला हे ॲप आणि आमचं काम आवडलं असेल, तर कृपया रिपॉझिटरीला स्टार द्या आणि आम्हाला फॉलो करा.
- **తెలుగు** (Telugu) — ☕ త్వరలో రాబోతోంది. · ⭐ మీకు ఈ యాప్, మా పని నచ్చితే, దయచేసి రిపోజిటరీకి ఒక స్టార్ ఇచ్చి మమ్మల్ని ఫాలో అవ్వండి.
- **தமிழ்** (Tamil) — ☕ விரைவில் வருகிறது. · ⭐ உங்களுக்கு இந்த ஆப்பும் எங்க வேலையும் பிடிச்சிருந்தா, தயவுசெஞ்சு இந்த ரெபாசிட்டரிக்கு ஒரு ஸ்டார்…
- **ગુજરાતી** (Gujarati) — ☕ ટૂંક સમયમાં આવી રહ્યું છે. · ⭐ જો તમને આ એપ અને અમારું કામ ગમતું હોય, તો પ્લીઝ રિપોઝિટરીને સ્ટાર આપો અને અમને ફોલો કરો.
- **ಕನ್ನಡ** (Kannada) — ☕ ಶೀಘ್ರದಲ್ಲೇ ಬರುತ್ತಿದೆ. · ⭐ ನಿಮಗೆ ಈ ಆ್ಯಪ್ ಮತ್ತು ನಮ್ಮ ಕೆಲಸ ಇಷ್ಟವಾದರೆ, ದಯವಿಟ್ಟು ರೆಪೊಸಿಟರಿಗೆ ಸ್ಟಾರ್ ಕೊಡಿ ಮತ್ತು ನಮ್ಮನ್ನು ಫಾಲೋ ಮಾಡಿ.
- **മലയാളം** (Malayalam) — ☕ ഉടൻ വരുന്നു. · ⭐ നിങ്ങൾക്ക് ഈ ആപ്പും ഞങ്ങളുടെ പ്രവർത്തനവും ഇഷ്ടപ്പെട്ടെങ്കിൽ, ദയവായി റെപ്പോസിറ്ററിക്ക് ഒരു സ്റ്റാർ…
- **ਪੰਜਾਬੀ** (Punjabi) — ☕ ਜਲਦੀ ਆ ਰਿਹਾ ਹੈ। · ⭐ ਜੇ ਤੁਹਾਨੂੰ ਇਹ ਐਪ ਅਤੇ ਸਾਡਾ ਕੰਮ ਪਸੰਦ ਹੈ, ਤਾਂ ਕਿਰਪਾ ਕਰਕੇ ਰਿਪੋਜ਼ਟਰੀ ਨੂੰ ਸਟਾਰ ਦਿਓ ਅਤੇ ਸਾਨੂੰ ਫਾਲੋ ਕਰੋ।
- **ଓଡ଼ିଆ** (Odia) — ☕ ଖୁବ୍ ଶୀଘ୍ର ଆସୁଛି। · ⭐ ଯଦି ଆପଣଙ୍କୁ ଏହି ଆପ୍ ଏବଂ ଆମର କାମ ଭଲ ଲାଗୁଛି, ତେବେ ଦୟାକରି ରିପୋଜିଟୋରୀକୁ ଗୋଟିଏ ଷ୍ଟାର୍ ଦିଅନ୍ତୁ ଏବଂ ଆମକୁ ଫଲୋ କରନ୍ତୁ।
- **অসমীয়া** (Assamese) — ☕ অতি সোনকালেই আহি আছে। · ⭐ যদি আপুনি এই এপটো আৰু আমাৰ কাম ভাল পায়, তেন্তে অনুগ্ৰহ কৰি ৰিপজিটৰীটোক এটা ষ্টাৰ দিয়ক আৰু আমাক ফ'ল' কৰক।
- **اردو** (Urdu) — ☕ جلد آرہا ہے۔ · ⭐ اگر آپ کو یہ ایپ اور ہمارا کام پسند آئے تو براہ کرم ریپازٹری کو سٹار دیں اور ہمیں فالو کریں۔
- **संस्कृतम्** (Sanskrit) — ☕ शीघ्रमेव आगच्छति।
- **मैथिली** (Maithili) — ☕ जल्दिये आबि रहल अछि।
- **कोंकणी** (Konkani) — ☕ रोखडेंच येता.
- **کٲشُر** (Kashmiri) — ☕ جَلدی یِوان چھُ۔
- **سنڌي** (Sindhi) — ☕ जल्द अची रह्यो आहे।
- **डोगरी** (Dogri) — ☕ जल्दी आ करदा ऐ।
- **ꯃꯤꯇꯩ ꯂꯣꯟ** (Manipuri) — ☕ ꯑꯊꯨꯕꯥ ꯃꯇꯝꯗꯥ ꯂꯥꯛꯀꯅꯤ꯫
- **बर’** (Bodo) — ☕ थाबैनो फैगोन।
- **ᱥᱟᱱᱛᱟᱲᱤ** (Santali) — ☕ ᱞᱚᱜᱚᱱ ᱦᱤᱡᱩᱜ ᱠᱟᱱᱟ᱾
- **नेपाली** (Nepali) — ☕ छिट्टै आउँदैछ। · ⭐ यदि तपाईंलाई यो एप र हाम्रो काम मन पर्‍यो भने, कृपया रिपोजिटरीलाई स्टार दिनुहोस् र हामीलाई फलो गर्नुहोस्।

**🌍 World languages**

- **العربية** (Arabic) — ☕ قريبًا. · ⭐ إذا أعجبك هذا التطبيق وعملنا، فيرجى منح المستودع نجمة ومتابعتنا.
- **简体中文** (Chinese (Simplified)) — ☕ 即将推出。
- **繁體中文** (Chinese (Traditional)) — ☕ 即將推出。
- **Čeština** (Czech) — ☕ Již brzy.
- **Dansk** (Danish) — ☕ Kommer snart.
- **Nederlands** (Dutch) — ☕ Binnenkort beschikbaar. · ⭐ Vind je deze app en ons werk leuk?
- **Filipino** (Filipino) — ☕ Malapit na.
- **Suomi** (Finnish) — ☕ Tulossa pian.
- **Français** (French) — ☕ Bientôt disponible. · ⭐ Si cette application et notre travail vous plaisent, mettez une étoile au dépôt et suivez-nous.
- **Deutsch** (German) — ☕ Bald verfügbar. · ⭐ Wenn dir diese App und unsere Arbeit gefallen, gib dem Repository einen Stern und folge uns.
- **Ελληνικά** (Greek) — ☕ Έρχεται σύντομα.
- **עברית** (Hebrew) — ☕ בקרוב.
- **Magyar** (Hungarian) — ☕ Hamarosan.
- **Bahasa Indonesia** (Indonesian) — ☕ Segera hadir. · ⭐ Jika kamu suka aplikasi ini dan karya kami, beri bintang pada repositori ini dan ikuti kami.
- **Italiano** (Italian) — ☕ In arrivo.
- **日本語** (Japanese) — ☕ 近日公開。 · ⭐ このアプリと私たちの仕事が気に入ったら、リポジトリにスターを付けてフォローしてください。
- **한국어** (Korean) — ☕ 곧 공개됩니다. · ⭐ 이 앱과 저희 작업이 마음에 드신다면 저장소에 스타를 눌러 주시고 팔로우해 주세요.
- **Bahasa Melayu** (Malay) — ☕ Akan datang.
- **Norsk** (Norwegian) — ☕ Kommer snart.
- **فارسی** (Persian) — ☕ به‌زودی.
- **Polski** (Polish) — ☕ Wkrótce. · ⭐ Jeśli podoba Ci się ta aplikacja i nasza praca, daj repozytorium gwiazdkę i obserwuj nas.
- **Português** (Portuguese) — ☕ Em breve.
- **Română** (Romanian) — ☕ În curând.
- **Русский** (Russian) — ☕ Скоро. · ⭐ Если вам нравится это приложение и наша работа, поставьте звёздочку репозиторию и подпишитесь на нас.
- **Español** (Spanish) — ☕ Muy pronto. · ⭐ Si te gusta esta app y nuestro trabajo, dale una estrella al repositorio y síguenos.
- **Kiswahili** (Swahili) — ☕ Inakuja hivi karibuni.
- **Svenska** (Swedish) — ☕ Kommer snart.
- **ไทย** (Thai) — ☕ เร็ว ๆ นี้
- **Türkçe** (Turkish) — ☕ Çok yakında.
- **Українська** (Ukrainian) — ☕ Незабаром.
- **Tiếng Việt** (Vietnamese) — ☕ Sắp ra mắt. · ⭐ Nếu bạn thích ứng dụng này và công việc của chúng tôi, hãy tặng kho lưu trữ một ngôi sao và theo…

</details>

