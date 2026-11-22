import os
import re
from pathlib import Path

SPACE_JAVA = Path(r"C:\Projects\Space-Modernized\src\main\java")

print("=== CHECKING PLANET ORES ===")
planet_ores = []
for root, dirs, files in os.walk(SPACE_JAVA):
    for f in files:
        if not f.endswith(".java"):
            continue
        c = (Path(root) / f).read_text(encoding="utf-8", errors="ignore")
        for m in re.finditer(r'PlanetOres\.ore\s*\(\s*"([^"]+)"\s*,\s*"([^"]+)"', c):
            ore, body = m.group(1), m.group(2)
            planet_ores.append((ore, body, f))

print(f"Total PlanetOres variants registered: {len(planet_ores)}")
bodies = set(b for o, b, f in planet_ores)
ores = set(o for o, b, f in planet_ores)
print(f"Bodies registering ores ({len(bodies)}): {sorted(bodies)}")
print(f"Ores registered as planet variants ({len(ores)}): {sorted(ores)}")
