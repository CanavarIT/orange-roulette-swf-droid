#!/usr/bin/env python3
"""
Extract the assets needed by this port from YOUR OWN copy of Orange_Roulette.swf.

Usage:
    python3 tools/extract_assets.py path/to/Orange_Roulette.swf

Requires: Python 3.8+, Pillow, numpy   (pip install pillow numpy)

Writes (all git-ignored):
    app/src/main/assets/gfx/*.png     sprite sheets (cropped to the used frames)
    app/src/main/assets/snd/*.mp3     sounds and music
    app/src/main/assets/text/story.txt
    app/src/main/res/drawable-nodpi/ic_orange.png   launcher icon
"""
import math
import os
import struct
import sys
import zlib

try:
    import numpy as np
    from PIL import Image
except ImportError:
    sys.exit("Please install dependencies first:  pip install pillow numpy")

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
RES = os.path.join(ROOT, "app", "src", "main", "res", "drawable-nodpi")

# sheet name -> (frame width, frame height, highest frame index used)
SHEETS = {
    "bluearm": (320, 425, 40), "bluearm2": (320, 425, 40),
    "head1_face1": (151, 139, 50), "head1_spikey": (225, 212, 8),
    "head2_face2": (159, 137, 18), "head2_face3": (159, 137, 18), "head2_face4": (159, 137, 18),
    "head2_face5": (120, 137, 2), "head2_spikey": (228, 220, 9),
    "cylinder_cylinder": (380, 400, 2), "firsty_neverplay": (276, 199, 5), "firsty_click": (254, 46, 2),
    "hand2_hand2": (78, 88, 7), "hand3_hand3": (78, 88, 7), "theend_endface": (88, 59, 1),
}
CROPS = {"ngtank_ngtank": (279, 62), "ngtank2_ngtank2": (200, 44)}
# identical to other images; the app maps these names to the originals
SKIP = {"bluearm2", "body2_body2", "firsty_firsty"}


def read_swf(path):
    d = open(path, "rb").read()
    sig = d[:3]
    if sig == b"CWS":
        body = zlib.decompress(d[8:])
    elif sig == b"FWS":
        body = d[8:]
    else:
        sys.exit("Unsupported SWF compression (expected CWS or FWS).")
    nbits = body[0] >> 3
    p = (5 + 4 * nbits + 7) // 8 + 4
    tags = []
    while p < len(body):
        h = struct.unpack("<H", body[p:p + 2])[0]
        p += 2
        t, l = h >> 6, h & 63
        if l == 63:
            l = struct.unpack("<I", body[p:p + 4])[0]
            p += 4
        tags.append((t, body[p:p + l]))
        p += l
    return tags


def lossless(x):
    cid, fmt = struct.unpack("<HB", x[:3])
    w, h = struct.unpack("<HH", x[3:7])
    q = 7
    ncol = 0
    if fmt == 3:
        ncol = x[7] + 1
        q = 8
    raw = zlib.decompress(x[q:])
    if fmt == 5:
        a = np.frombuffer(raw, dtype=np.uint8).reshape(h, w, 4)
        alpha = a[..., 0].astype(np.float32)
        rgb = a[..., 1:4].astype(np.float32)
    elif fmt == 3:
        pal = np.frombuffer(raw[:ncol * 4], dtype=np.uint8).reshape(ncol, 4)
        rw = (w + 3) & ~3
        idx = np.frombuffer(raw[ncol * 4:], dtype=np.uint8).reshape(h, rw)[:, :w]
        a = pal[idx]
        alpha = a[..., 3].astype(np.float32)
        rgb = a[..., :3].astype(np.float32)
    else:
        return cid, None
    rgb = np.where(alpha[..., None] > 0, np.minimum(rgb * 255.0 / np.maximum(alpha[..., None], 1), 255), 0)
    out = np.dstack([rgb, alpha]).astype(np.uint8)
    return cid, Image.fromarray(out, "RGBA")


def symbols(x):
    n = struct.unpack("<H", x[:2])[0]
    q = 2
    out = {}
    for _ in range(n):
        i = struct.unpack("<H", x[q:q + 2])[0]
        q += 2
        z = x.index(b"\0", q)
        out[x[q:z].decode()] = i
        q = z + 1
    return out


# ---- minimal ABC constant-pool reader (only used to read story text) ----
def abc_strings(x):
    p = 4
    def u32():
        nonlocal p
        r = sh = 0
        while True:
            c = x[p]; p += 1
            r |= (c & 127) << sh; sh += 7
            if not c & 128:
                return r & 0xFFFFFFFF
    for _ in range(max(u32() - 1, 0)): u32()          # ints
    for _ in range(max(u32() - 1, 0)): u32()          # uints
    for _ in range(max(u32() - 1, 0)): p += 8         # doubles
    n = u32()
    out = [""]
    for _ in range(max(n - 1, 0)):
        l = u32()
        out.append(x[p:p + l].decode("utf8", "replace"))
        p += l
    return out


def story_groups(tags):
    """Text arrays are built in the constructors as: pushstring i, pushstring j, ..., newarray n.
    Reading them from the bytecode keeps the exact order and repeated lines."""
    import re
    out = {}
    dl = rb"(?:\xf0(?:[\x80-\xff][\x00-\x7f]|[\x00-\x7f]))?"   # optional debugline between pushes
    pat = re.compile(rb"((?:\x2c[\x01-\x7f]" + dl + rb")+)\x56([\x01-\x7f])", re.S)
    for t, x in tags:
        if t not in (72, 82):
            continue
        body = x
        if t == 82:
            body = x[x.index(b"\0", 4) + 1:]
        try:
            s = abc_strings(body)
        except Exception:
            continue
        names = None
        if "src:HUD" in s:
            names = ["waves", "waves2", "waves3", "waves4", "waves5"]
        elif "src:Deceased" in s:
            names = ["deceased"]
        if not names:
            continue
        arrays = []
        for m in pat.finditer(body):
            idxs = re.findall(rb"\x2c([\x01-\x7f])", m.group(1))
            idxs = [c[0] for c in idxs]
            if len(idxs) == m.group(2)[0] and len(idxs) >= 3:
                arrays.append([s[i] for i in idxs])
        for n, arr in zip(names, arrays):
            out[n] = arr
    return out


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    tags = read_swf(sys.argv[1])
    syms = {}
    images = {}
    sounds = {}
    for t, x in tags:
        if t == 76:
            syms.update(symbols(x))
        elif t == 36:
            cid, im = lossless(x)
            if im is not None:
                images[cid] = im
        elif t == 14:
            cid = struct.unpack("<H", x[:2])[0]
            fmt = x[2] >> 4
            if fmt == 2:
                sounds[cid] = x[9:]       # skip header + 2-byte MP3 seek value

    os.makedirs(os.path.join(ASSETS, "gfx"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "snd"), exist_ok=True)
    os.makedirs(os.path.join(ASSETS, "text"), exist_ok=True)
    os.makedirs(RES, exist_ok=True)

    ng = 0
    for name, cid in syms.items():
        if cid in images and name.startswith("src.") and "_img" in name:
            cls = name.split(".")[1].split("_")[0]
            img = name.split("_img")[1]
            key = (cls + "_" + img).lower()
            if key == "bluearm_bluearm":
                key = "bluearm"
            elif key == "bluearm2_bluearm2":
                key = "bluearm2"
            if key in SKIP:
                continue
            im = images[cid]
            if key in SHEETS:
                fw, fh, mx = SHEETS[key]
                cols = im.width // fw
                rows = math.ceil((mx + 1) / cols)
                im = im.crop((0, 0, cols * fw, min(im.height, rows * fh)))
            elif key in CROPS:
                im = im.crop((0, 0) + CROPS[key])
            im.save(os.path.join(ASSETS, "gfx", key + ".png"), optimize=True)
            ng += 1
            if key == "hud_prison":
                s = max(im.size)
                c = Image.new("RGBA", (s, s), (0, 0, 0, 0))
                c.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
                c.resize((192, 192), Image.LANCZOS).save(os.path.join(RES, "ic_orange.png"))
        elif cid in sounds:
            fname = name.split("_")[-1].lower()
            with open(os.path.join(ASSETS, "snd", fname + ".mp3"), "wb") as f:
                f.write(sounds[cid])

    groups = story_groups(tags)
    with open(os.path.join(ASSETS, "text", "story.txt"), "w", encoding="utf-8") as f:
        for k in ("waves", "waves2", "waves3", "waves4", "waves5", "deceased"):
            for line in groups.get(k, []):
                f.write(f"{k}\t{line}\n")

    print(f"Done: {ng} images, {len(sounds)} sounds, {sum(len(v) for v in groups.values())} story lines.")
    print("Optional: put the free pixel font 04B_03 at app/src/main/assets/font/04b03.ttf")


if __name__ == "__main__":
    main()
