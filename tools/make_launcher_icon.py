#!/usr/bin/env python3
"""
Збирає adaptive icon із MainIcon.png (плаский знак на білому тлі, без альфи).

    python3 tools/make_launcher_icon.py --mark '#8CC1AD' --background '#FFFFFF'

Альфа відновлюється з того, що знак однотонний і лежить на однотонному тлі:
для P = a*C + (1-a)*W маємо a = (W-P)/(W-C). Береться червоний канал — у нього
найбільша різниця між тлом і знаком, тож маска виходить найстабільніша,
а згладжування країв зберігається.
"""
import argparse
from PIL import Image

CANVAS = 432        # 108dp @ xxxhdpi
SAFE = 0.56         # гарантована безпечна зона adaptive icon — 66/108; беремо з запасом
NOISE_CUT = 12      # тло у вихідному файлі шумне; без відсічки bbox бере весь кадр
RES = "app/src/main/res"


def rgb(value: str) -> tuple[int, int, int]:
    v = value.lstrip("#")
    return int(v[0:2], 16), int(v[2:4], 16), int(v[4:6], 16)


def luminance(c: tuple[int, int, int]) -> float:
    def channel(x: float) -> float:
        x /= 255
        return x / 12.92 if x <= 0.04045 else ((x + 0.055) / 1.055) ** 2.4
    r, g, b = (channel(x) for x in c)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b


def contrast(a: tuple[int, int, int], b: tuple[int, int, int]) -> float:
    la, lb = luminance(a), luminance(b)
    hi, lo = max(la, lb), min(la, lb)
    return (hi + 0.05) / (lo + 0.05)


def build_mask(source: str) -> Image.Image:
    src = Image.open(source).convert("RGB")
    bg_level = src.getpixel((5, 5))[0]
    mark_level = src.getpixel((src.width // 2, src.height // 2))[0]
    span = bg_level - mark_level
    if abs(span) < 20:
        raise SystemExit(f"тло і знак надто близькі по червоному каналу ({bg_level} vs {mark_level})")

    alpha = src.split()[0].point(
        lambda p: (lambda a: 0 if a < NOISE_CUT else a)(max(0, min(255, round((bg_level - p) / span * 255))))
    )
    mask = alpha.crop(alpha.getbbox())
    scale = int(CANVAS * SAFE) / max(mask.size)
    return mask.resize((round(mask.width * scale), round(mask.height * scale)), Image.LANCZOS)


def layer(mask: Image.Image, color: tuple[int, int, int]) -> Image.Image:
    out = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    mark = Image.new("RGBA", mask.size, color + (0,))
    mark.putalpha(mask)
    out.paste(mark, ((CANVAS - mask.width) // 2, (CANVAS - mask.height) // 2), mark)
    return out


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--source", default="MainIcon.png")
    ap.add_argument("--mark", required=True, help="колір знака, напр. #8CC1AD")
    ap.add_argument("--background", required=True, help="колір тла, напр. #FFFFFF")
    args = ap.parse_args()

    mark, background = rgb(args.mark), rgb(args.background)
    mask = build_mask(args.source)

    layer(mask, mark).save(f"{RES}/mipmap-xxxhdpi/ic_launcher_foreground.png")
    layer(mask, (0, 0, 0)).save(f"{RES}/mipmap-xxxhdpi/ic_launcher_monochrome.png")
    with open(f"{RES}/values/ic_launcher_colors.xml", "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
                f'    <color name="ic_launcher_background">{args.background.upper()}</color>\n'
                '</resources>\n')

    ratio = contrast(mark, background)
    verdict = "ок" if ratio >= 3 else f"нижче порога 3:1 для великої графіки"
    print(f"знак {args.mark} на тлі {args.background}: контраст {ratio:.2f}:1 — {verdict}")
    print(f"контент {max(mask.size)}px у полотні {CANVAS} ({max(mask.size)/CANVAS:.0%}, безпечна зона 61%)")


if __name__ == "__main__":
    main()
