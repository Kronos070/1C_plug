import struct
import sys
import zlib
from pathlib import Path

PIXELS: tuple[str, ...] = ("ccyy", "c..y", "c..y", "c..y")
COLORS: dict[str, tuple[int, int, int]] = {"c": (228, 138, 122), "y": (234, 182, 93)}
ICONS: tuple[tuple[str, int, int], ...] = (("perun.png", 16, 3), ("perun@2x.png", 32, 7))
DEFAULT_TARGET: Path = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "icons"


def pixel(x: int, y: int, margin: int, cell: int) -> bytes:
    """Возвращает цвет точки иконки."""
    column, row = (x - margin) // cell, (y - margin) // cell
    if 0 <= column < len(PIXELS[0]) and 0 <= row < len(PIXELS) and x >= margin and y >= margin:
        color = COLORS.get(PIXELS[row][column])
        if color is not None:
            return bytes((*color, 255))
    return bytes(4)


def chunk(kind: bytes, data: bytes) -> bytes:
    """Упаковывает блок данных PNG."""
    checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)


def render(size: int, cell: int) -> bytes:
    """Рисует букву П в PNG."""
    margin = (size - cell * len(PIXELS[0])) // 2
    rows = b"".join(
        b"\x00" + b"".join(pixel(x, y, margin, cell) for x in range(size)) for y in range(size)
    )
    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", header)
        + chunk(b"IDAT", zlib.compress(rows, 9))
        + chunk(b"IEND", b"")
    )


def main() -> None:
    """Сохраняет иконки плагина в папку."""
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_TARGET
    target.mkdir(parents=True, exist_ok=True)
    for name, size, cell in ICONS:
        (target / name).write_bytes(render(size, cell))
        print(f"{target / name}: {size}x{size}")


if __name__ == "__main__":
    main()
