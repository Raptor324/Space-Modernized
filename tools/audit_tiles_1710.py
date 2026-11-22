import os
import re
from pathlib import Path

NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")

def get_tile_mappings(repo_path):
    f = repo_path / "src/main/java/com/hbm/tileentity/TileMappings.java"
    if not f.exists():
        return {}
    c = f.read_text(encoding="utf-8", errors="ignore")
    mappings = {}
    for m in re.finditer(r'put\s*\(\s*([A-Za-z0-9_]+)\.class\s*,\s*"([^"]+)"\s*\)', c):
        mappings[m.group(2)] = m.group(1)
    return mappings

ntm_tiles = get_tile_mappings(NTM_1710)
git_tiles = get_tile_mappings(HBM_GIT)

space_tiles = {k: v for k, v in ntm_tiles.items() if k not in git_tiles}
print(f"Total tile entities in 1.7.10 NTM: {len(ntm_tiles)}")
print(f"Total tile entities in 1.7.10 HBM-GIT: {len(git_tiles)}")
print(f"Space-specific tile entities in TileMappings: {len(space_tiles)}")
for k, v in sorted(space_tiles.items()):
    print(f"  id='{k}' -> class={v}")
