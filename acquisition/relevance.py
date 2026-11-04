"""Relevance + licence gate applied to every discovery candidate.

Two independent checks must pass before a recording is downloaded:
  1. LICENCE  - see license_policy.classify (CC0 / PD / CC BY / CC BY-SA only)
  2. SUBJECT  - the file must actually be the sound it claims to be, and must
                not be a music track, spoken-word item or radio recording.
"""
import re

MUSIC = re.compile(
    r"\b(medley|rag|rags|ragtime|waltz|lullaby|poem|symphon\w*|quartet|inleiding|banda|chopin|"
    r"debussy|jazz|blues|orchestra|orchestral|piano|guitar|sonata|aria|choir|hymn|anthem|ballad|"
    r"fox ?trot|foxtrot|vaudeville|minstrel|serenade|opus|march|overture|concerto|polka|mazurka|"
    r"opera|tenor|soprano|baritone|songbook|album|track ?\d|remix|beat|edm|pop song|folk song|"
    r"podcast|interview|lecture|speech|radio|audiobook|story|chapter|president|court|opinion|"
    r"remarks|reflects|article|episode|conference|panel|seminar|documentary|trailer|soundtrack|"
    r"\bost\b|hogwarts|avatar|anime|gameplay|wiki|ted talk|sermon|election|propeller|"
    r"zero-project|loyalist|daniel birch|greg walker|buryme|alouette|war|battle|anthem|hymn)\b", re.I)
YEAR = re.compile(r"\b(18\d\d|19\d\d|20[0-2]\d)\b")
XC = re.compile(r"\bXC\d{4,}\b", re.I)
BAD_HOST = re.compile(r"(soundcloud|beeld en geluid|muziekweb|radio ?nederland)", re.I)

GROUP_TOKENS = {
    "Wind": ["wind", "breeze", "gale", "gust", "zephyr", "vent"],
    "Rain": ["rain", "drizzle", "downpour", "monsoon", "shower", "precipitation", "raindrop", "raining"],
    "Water": ["water", "river", "stream", "creek", "brook", "waterfall", "flowing", "flow", "spring",
              "fountain", "drip", "rapids", "cascade", "beck", "burn", "rill"],
    "Ocean": ["wave", "sea", "ocean", "surf", "beach", "shore", "tide", "coast", "harbour", "harbor",
              "estuary", "bay", "lagoon"],
    "Fire": ["fire", "campfire", "bonfire", "crackl", "flame", "ember", "burning", "fireplace"],
    "Storm": ["thunder", "storm", "lightning", "hailstorm", "tempest", "thunderstorm"],
    "Night": ["night", "cricket", "nocturn", "owl", "katydid", "evening", "dusk", "nocturnal"],
    "Forest": ["forest", "wood", "woodland", "jungle", "rainforest", "pine", "grove", "bush",
               "thicket", "canopy", "leaf", "leaves", "tree", "trees", "taiga", "bamboo"],
    "Birds": ["bird", "birds", "song", "songbird", "call", "chirp", "warbler", "owl", "eagle", "duck",
              "goose", "gull", "parrot", "quail", "lark", "thrush", "robin", "kookaburra",
              "kingfisher", "hummingbird", "loon", "crow", "sparrow", "finch", "wren", "swallow",
              "hawk", "falcon", "crane", "heron", "pelican", "swan", "pigeon", "dove", "magpie",
              "jay", "cuckoo", "nightjar", "barbet", "tinamou", "guan", "lyrebird", "whipbird",
              "pipit", "oriole", "tanager", "chorus", "birdsong"],
    "Wildlife": ["animal", "frog", "insect", "cricket", "bee", "wasp", "bat", "whale", "dolphin",
                 "horse", "cow", "sheep", "goat", "elephant", "zebra", "kangaroo", "koala", "otter",
                 "monkey", "bird", "call", "chirp", "buzz", "croak", "howl", "growl", "hoot",
                 "squirrel", "wildlife", "fauna"],
    "Insects": ["insect", "cricket", "cicada", "bee", "wasp", "grasshopper", "katydid", "buzz",
                "mosquito", "hoverfly", "bumblebee", "bug"],
    "Ambience": [],  # relies on name/tag tokens
}

STOP = {"the", "and", "with", "sound", "sounds", "nature", "natural", "ambience", "ambiance",
        "of", "in", "on", "a", "for", "quiet", "gentle", "soft", "light", "heavy", "deep",
        "distant", "roaring", "pouring", "flowing", "singing", "chirping", "rustling", "calls",
        "morning", "evening", "night", "day", "exotic", "wild"}


def _tokens(text):
    return [t for t in re.findall(r"[a-z]+", (text or "").lower()) if len(t) > 2]


def subject_ok(name, group, tags, title, description=""):
    """True when the candidate plausibly IS the requested sound."""
    t = (title or "").lower()
    if not t:
        return False
    if BAD_HOST.search(t):
        return False
    if MUSIC.search(t):
        return False
    if YEAR.search(t):
        return False
    if XC.search(title or "") and group != "Birds":
        return False
    if group != "Birds" and re.search(r"\bsongs?\b", t) and not re.search(r"\b(bird|frog|whale|insect)", t):
        return False

    name_tok = [x for x in _tokens(name) if x not in STOP]
    tag_tok = [x for x in tags if x not in STOP]
    group_tok = GROUP_TOKENS.get(group, [])

    # a specific name token in the title is the strongest signal
    if any(tok in t for tok in name_tok):
        return True
    if any(tok in t for tok in group_tok):
        return True
    if any(tok in t for tok in tag_tok):
        # tags alone are weaker: require an audio-ish word too
        return True
    return False


def relevance(name, group, tags, title):
    t = (title or "").lower()
    s = 0.0
    for tok in _tokens(name):
        if tok in STOP:
            continue
        if tok in t:
            s += 14
    for tok in GROUP_TOKENS.get(group, []):
        if tok in t:
            s += 5
    for tok in tags:
        if tok in t:
            s += 3
    if re.search(r"(ambien|sound|record|field recording|atmos)", t):
        s += 6
    if re.search(r"(sound effect|soundscape|noise)", t):
        s += 3
    if len(t) < 45:
        s += 2
    return s
