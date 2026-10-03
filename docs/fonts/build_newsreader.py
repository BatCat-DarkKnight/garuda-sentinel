"""Rebuilds app/src/main/res/font/newsreader_*.ttf from the Google Fonts variable font.

Needs fontTools (pip install fonttools). Download the source first:
  https://raw.githubusercontent.com/google/fonts/main/ofl/newsreader/Newsreader%5Bopsz%2Cwght%5D.ttf
saved as Newsreader-VF.ttf next to this script, then run:
  python build_newsreader.py

Each weight is pinned to a static instance at optical size 30 (between the title and the
display sizes the app uses) and subset to Latin. Newsreader has no Reserved Font Name, so
the modified files keep the name. IBM Plex is not processed here: it has the Reserved Font
Name "Plex", so its files ship exactly as Google Fonts publishes them.
"""
from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

# Google Fonts' "latin" range plus Latin Extended-A, so Central European names in app and
# file names stay in one typeface.
UNICODES = (
    "U+0000-00FF,U+0100-017F,U+0131,U+0152-0153,U+02BB-02BC,U+02C6,U+02DA,U+02DC,U+0304,"
    "U+0308,U+0329,U+2000-206F,U+20AC,U+2122,U+2191,U+2193,U+2212,U+2215,U+FEFF,U+FFFD"
)
OPTICAL_SIZE = 30
WEIGHTS = [("regular", "Regular", 400), ("medium", "Medium", 500), ("semibold", "SemiBold", 600)]

for file_suffix, style, weight in WEIGHTS:
    font = instancer.instantiateVariableFont(TTFont("Newsreader-VF.ttf"), {"wght": weight, "opsz": OPTICAL_SIZE})
    names = font["name"]
    for name_id in (16, 17, 25):
        names.removeNames(nameID=name_id)
    names.setName("Newsreader" if style == "Regular" else f"Newsreader {style}", 1, 3, 1, 0x409)
    names.setName("Regular", 2, 3, 1, 0x409)
    names.setName("Newsreader", 16, 3, 1, 0x409)
    names.setName(style, 17, 3, 1, 0x409)
    names.setName(f"Newsreader {style}", 4, 3, 1, 0x409)
    names.setName(f"Newsreader-{style}", 6, 3, 1, 0x409)
    font["OS/2"].usWeightClass = weight

    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]  # keeps the copyright (0) and licence (13, 14) records
    options.name_languages = ["*"]
    options.notdef_outline = True
    options.hinting = False
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=subset.parse_unicodes(UNICODES))
    subsetter.subset(font)
    font.save(f"newsreader_{file_suffix}.ttf")
