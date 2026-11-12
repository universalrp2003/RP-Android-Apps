#!/usr/bin/env python3
"""Reject inherited purple foregrounds and unreadable launcher button palettes."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'launcher/src/main/java/com/universalrp/launcher/MainActivity.kt'
PALETTE = ROOT / 'launcher/src/main/java/com/universalrp/launcher/LauncherPalette.kt'


def contrast(first, second):
    def luminance(value):
        channels = [(value >> shift & 255) / 255 for shift in (16, 8, 0)]
        linear = [c / 12.92 if c <= .04045 else ((c + .055) / 1.055) ** 2.4 for c in channels]
        return sum(c * w for c, w in zip(linear, (.2126, .7152, .0722)))
    light, dark = sorted((luminance(first), luminance(second)), reverse=True)
    return (light + .05) / (dark + .05)


def verify(source, palette):
    calls = re.findall(r'ButtonDefaults\.buttonColors\(([^)]*)\)', source, re.S)
    if not calls:
        raise ValueError('No filled-button colors matched; refusing to verify nothing')
    for call in calls:
        if not re.search(r'(?<!\w)contentColor\s*=\s*ButtonText\b', call):
            raise ValueError('Filled button inherits its foreground instead of ButtonText')
        if not re.search(r'disabledContentColor\s*=\s*Muted\b', call):
            raise ValueError('Disabled button has no explicit readable foreground')
    if 'Color(LauncherPalette.BUTTON_TEXT)' not in source:
        raise ValueError('ButtonText is not wired to the verified palette')
    values = {key: int(value, 16) for key, value in re.findall(r'const val (\w+) = 0x([A-F0-9]+)L', palette)}
    for foreground, background in [('BUTTON_TEXT', 'CARD'), ('BUTTON_TEXT', 'FAVOURITE'), ('BUTTON_TEXT', 'EMERGENCY'), ('MUTED', 'CARD'), ('ACCENT', 'CARD'), ('BACKGROUND', 'ACCENT')]:
        ratio = contrast(values[foreground], values[background])
        if ratio < 4.5:
            raise ValueError(f'{foreground}/{background} contrast {ratio:.2f}:1 is below 4.5:1')
    return len(calls)


if __name__ == '__main__':
    count = verify(SOURCE.read_text(), PALETTE.read_text())
    print(f'PASS: {count} launcher filled-button paths have explicit foregrounds; all six palette pairs meet 4.5:1')
