# Генерирует иконки поля редактора для правок ИИ: добавлено, изменено, удалено
import struct
import sys
import zlib
from pathlib import Path

COLORS: dict[str, tuple[int, int, int]] = {
    "ai-added": (120, 132, 230),
    "ai-modified": (232, 146, 52),
    "ai-deleted": (214, 72, 64),
}
SIZES: tuple[tuple[str, int], ...] = (("", 16), ("@2x", 32))
DEFAULT_TARGET: Path = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "icons"


def inside(name: str, x: int, y: int, size: int) -> bool:
    """Проверяет, закрашена ли точка иконки."""
    scale = size / 16
    if name == "ai-deleted":
        middle = size / 2
        reach = 4.5 * scale - abs(y + 0.5 - middle)
        return reach > 0 and 3 * scale <= x + 0.5 <= 3 * scale + reach * 1.2
    left, right = 5 * scale, 8 * scale
    top, bottom = 1 * scale, 15 * scale
    return left <= x + 0.5 <= right and top <= y + 0.5 <= bottom


def chunk(kind: bytes, data: bytes) -> bytes:
    """Упаковывает блок данных PNG."""
    checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)


def render(name: str, size: int) -> bytes:
    """Рисует иконку правки в PNG."""
    color = bytes((*COLORS[name], 255))
    rows = b"".join(
        b"\x00" + b"".join(color if inside(name, x, y, size) else bytes(4) for x in range(size))
        for y in range(size)
    )
    header = struct.pack(">IIBBBBB", size, size, 8, 6, 0, 0, 0)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(rows, 9)) + chunk(b"IEND", b"")


def main() -> None:
    """Сохраняет иконки правок в папку."""
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_TARGET
    target.mkdir(parents=True, exist_ok=True)
    for name in COLORS:
        for suffix, size in SIZES:
            path = target / f"{name}{suffix}.png"
            path.write_bytes(render(name, size))
            print(f"{path}: {size}x{size}")


if __name__ == "__main__":
    main()
