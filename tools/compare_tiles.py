import os
import re
from pathlib import Path
from scan_registrations import scan_registrations

NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")
SPACE_MOD = Path(r"C:\Projects\Space-Modernized")

# Load space tiles from 1.7.10
from audit_tiles_1710 import space_tiles

blocks, items, modern_bes, fluids, entities, menus = scan_registrations()

print("=== TILE ENTITY / BLOCK ENTITY PARITY ===")
print(f"1.7.10 Space Tile Entities in TileMappings: {len(space_tiles)}")
print(f"Space-Modernized Block Entities Registered: {len(modern_bes)}")

matched_bes = []
missing_bes = []

for tile_id, cls_name in sorted(space_tiles.items()):
    clean_id = tile_id.replace("tileentity_", "").lower()
    # check candidates
    found = None
    for cand in [clean_id, tile_id, clean_id.replace("_", "")]:
        if cand in modern_bes:
            found = modern_bes[cand]
            break
    # also try class name match
    if not found:
        for be_id, be_info in modern_bes.items():
            if cls_name.lower() in be_info.get('type', '').lower():
                found = be_info
                break

    if found:
        matched_bes.append((tile_id, cls_name, found))
    else:
        missing_bes.append((tile_id, cls_name))

print(f"\nPorted in Space-Modernized: {len(matched_bes)} / {len(space_tiles)} ({len(matched_bes)/len(space_tiles)*100:.1f}%)")
print(f"Missing Block Entities: {len(missing_bes)} / {len(space_tiles)}")

print("\n--- Ported Block Entities ---")
for tid, cls, found in matched_bes:
    print(f"  {tid} ({cls}) -> {found['id']} in {found.get('file', '?')}")

print("\n--- Missing Block Entities ---")
for tid, cls in missing_bes:
    print(f"  {tid} ({cls})")
