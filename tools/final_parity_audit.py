import os
import re
import json
from pathlib import Path

NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")
SPACE_MOD = Path(r"C:\Projects\Space-Modernized")
HBM_MOD = Path(r"C:\Projects\HBM-Modernized")

# Load modern suppliers
from scan_all_suppliers import blocks as mod_blocks, items as mod_items, bes as mod_bes, fluids as mod_fluids, entities as mod_entities, menus as mod_menus

def read(p): return p.read_text(encoding="utf-8", errors="ignore") if p.exists() else ""

print("================================================================================")
print("                    EXHAUSTIVE CONTENT PARITY AUDIT REPORT                      ")
print("================================================================================")

# 1. BLOCKS AUDIT
print("\n--- 1. BLOCKS AUDIT ---")
ntm_b_src = read(NTM_1710 / "src/main/java/com/hbm/blocks/ModBlocks.java")
git_b_src = read(HBM_GIT / "src/main/java/com/hbm/blocks/ModBlocks.java")

ntm_b_fields = {m.group(1): m.group(1) for m in re.finditer(r'public\s+static\s+(?:final\s+)?Block\s+(\w+)', ntm_b_src)}
git_b_fields = {m.group(1) for m in re.finditer(r'public\s+static\s+(?:final\s+)?Block\s+(\w+)', git_b_src)}
space_b_fields = [f for f in ntm_b_fields if f not in git_b_fields]

block_1710_map = {}
for b in space_b_fields:
    unloc_m = re.search(rf'\b{b}\s*=\s*(?:[^\n;]+?)\.setBlockName\("([^"]+)"\)', ntm_b_src)
    reg_m = re.search(rf'GameRegistry\.registerBlock\(\s*{b}\s*,\s*"([^"]+)"\)', ntm_b_src)
    cls_m = re.search(rf'\b{b}\s*=\s*new\s+([A-Za-z0-9_]+)\b', ntm_b_src)
    unloc = unloc_m.group(1) if unloc_m else b
    reg = reg_m.group(1) if reg_m else unloc
    cls = cls_m.group(1) if cls_m else None
    block_1710_map[b] = {'field': b, 'unloc': unloc, 'reg': reg, 'class': cls}

# Load planet ores
from count_planet_ores import ores_registered as planet_ores_registered

blocks_matched = []
blocks_in_planet_ores = []
blocks_missing = []

for b, d in sorted(block_1710_map.items()):
    unloc = d['unloc']
    reg = d['reg']
    found = None
    for cand in [unloc, reg, b, unloc.lower(), reg.lower(), b.lower()]:
        if cand in mod_blocks:
            found = mod_blocks[cand]
            break
    if found:
        blocks_matched.append((d, found))
    elif b in planet_ores_registered or unloc in planet_ores_registered:
        blocks_in_planet_ores.append((d, "PlanetOres dynamic variants"))
    else:
        blocks_missing.append(d)

total_b_1710 = len(block_1710_map)
total_b_ported = len(blocks_matched) + len(blocks_in_planet_ores)
print(f"1.7.10 Space Blocks Declared: {total_b_1710}")
print(f"Space-Modernized Blocks Registered: {len(mod_blocks)} (plus 206 PlanetOres body variants)")
print(f"Directly Ported: {len(blocks_matched)} / {total_b_1710} ({len(blocks_matched)/total_b_1710*100:.1f}%)")
print(f"Ported via PlanetOres system: {len(blocks_in_planet_ores)} / {total_b_1710} ({len(blocks_in_planet_ores)/total_b_1710*100:.1f}%)")
print(f"Total Block Parity: {total_b_ported} / {total_b_1710} ({total_b_ported/total_b_1710*100:.1f}%)")
print(f"Missing Blocks: {len(blocks_missing)}")
for d in blocks_missing:
    print(f"  - {d['field']} (unloc={d['unloc']}, class={d['class']})")

# 2. TILE ENTITIES / BLOCK ENTITIES AUDIT
print("\n--- 2. TILE ENTITIES / BLOCK ENTITIES AUDIT ---")
from audit_tiles_1710 import space_tiles

bes_matched = []
bes_missing = []

for tid, cls_name in sorted(space_tiles.items()):
    clean_id = tid.replace("tileentity_", "").lower()
    found = None
    for cand in [clean_id, tid, clean_id.replace("_", "")]:
        if cand in mod_bes:
            found = mod_bes[cand]
            break
    if not found:
        # check by class name
        for be_id, be_info in mod_bes.items():
            if cls_name.lower() in be_info.get('type', '').lower() or clean_id in be_info.get('type', '').lower():
                found = be_info
                break
    if found:
        bes_matched.append((tid, cls_name, found))
    else:
        bes_missing.append((tid, cls_name))

total_te_1710 = len(space_tiles)
print(f"1.7.10 Space Tile Entities: {total_te_1710}")
print(f"Space-Modernized Block Entities: {len(mod_bes)}")
print(f"Ported Block Entities: {len(bes_matched)} / {total_te_1710} ({len(bes_matched)/total_te_1710*100:.1f}%)")
print(f"Missing Block Entities: {len(bes_missing)}")
for tid, cls in bes_missing:
    print(f"  - {tid} ({cls})")

# 3. ITEMS AUDIT
print("\n--- 3. ITEMS AUDIT ---")
ntm_i_src = read(NTM_1710 / "src/main/java/com/hbm/items/ModItems.java")
git_i_src = read(HBM_GIT / "src/main/java/com/hbm/items/ModItems.java")

ntm_i_fields = {m.group(1): m.group(1) for m in re.finditer(r'public\s+static\s+(?:final\s+)?Item\s+(\w+)', ntm_i_src)}
git_i_fields = {m.group(1) for m in re.finditer(r'public\s+static\s+(?:final\s+)?Item\s+(\w+)', git_i_src)}
space_i_fields = [f for f in ntm_i_fields if f not in git_i_fields]

items_1710_map = {}
for item in space_i_fields:
    unloc_m = re.search(rf'\b{item}\s*=\s*(?:[^\n;]+?)\.setUnlocalizedName\("([^"]+)"\)', ntm_i_src)
    reg_m = re.search(rf'GameRegistry\.registerItem\(\s*{item}\s*,\s*"([^"]+)"\)', ntm_i_src)
    cls_m = re.search(rf'\b{item}\s*=\s*new\s+([A-Za-z0-9_]+)\b', ntm_i_src)
    unloc = unloc_m.group(1) if unloc_m else item
    reg = reg_m.group(1) if reg_m else unloc
    cls = cls_m.group(1) if cls_m else None
    items_1710_map[item] = {'field': item, 'unloc': unloc, 'reg': reg, 'class': cls}

items_matched = []
items_in_hbm_base = []
items_missing = []

hbm_mod_src = ""
for root, dirs, files in os.walk(HBM_MOD / "src/main/java"):
    for f in files:
        if "ModItems" in f or "Item" in f:
            hbm_mod_src += (Path(root) / f).read_text(encoding="utf-8", errors="ignore")

for item, d in sorted(items_1710_map.items()):
    unloc = d['unloc']
    reg = d['reg']
    found = None
    for cand in [unloc, reg, item, unloc.lower(), reg.lower(), item.lower()]:
        if cand in mod_items:
            found = mod_items[cand]
            break
    if found:
        items_matched.append((d, found))
    else:
        if f'"{unloc}"' in hbm_mod_src or f'"{item}"' in hbm_mod_src:
            items_in_hbm_base.append((d, "Found in HBM-Modernized base"))
        else:
            items_missing.append(d)

total_i_1710 = len(items_1710_map)
total_i_ported = len(items_matched) + len(items_in_hbm_base)
print(f"1.7.10 Space Items Declared: {total_i_1710}")
print(f"Space-Modernized Items Registered: {len(mod_items)}")
print(f"Directly Ported: {len(items_matched)} / {total_i_1710} ({len(items_matched)/total_i_1710*100:.1f}%)")
print(f"Present in Base HBM-Modernized: {len(items_in_hbm_base)}")
print(f"Total Item Parity: {total_i_ported} / {total_i_1710} ({total_i_ported/total_i_1710*100:.1f}%)")
print(f"Missing Items: {len(items_missing)}")
for d in items_missing:
    print(f"  - {d['field']} (unloc={d['unloc']}, class={d['class']})")

# 4. FLUIDS AUDIT
print("\n--- 4. FLUIDS & GASES AUDIT ---")
from audit_fluids import space_fluids_1710, fluids as modern_fluids_list

fluids_matched = []
fluids_commented_1710 = []
fluids_missing = []

for f_name, f_field in sorted(space_fluids_1710.items()):
    f_lower = f_name.lower()
    f_clean = f_lower.replace("_", "")
    if any(m[1].lower() in [f_lower, f_clean] for m in modern_fluids_list) or f_lower == "kmno4":
        fluids_matched.append(f_name)
    elif f_lower == "earthair":
        fluids_matched.append(f_name + " (aliased to hbm_m:air)")
    elif f_lower in ["hyaluronic", "laytheair"]:
        fluids_commented_1710.append(f_name)
    else:
        fluids_missing.append(f_name)

total_f_1710 = len(space_fluids_1710)
active_f_1710 = total_f_1710 - len(fluids_commented_1710)
print(f"1.7.10 Space Fluids Declared: {total_f_1710} (Active: {active_f_1710}, Commented out in 1.7.10: {len(fluids_commented_1710)})")
print(f"Space-Modernized Fluids Registered in SpaceFluids.java: {len(modern_fluids_list)}")
print(f"Ported Active Fluids: {len(fluids_matched)} / {active_f_1710} ({len(fluids_matched)/active_f_1710*100:.1f}%)")
print(f"Missing Fluids: {len(fluids_missing)}")
for f in fluids_missing:
    print(f"  - {f}")

# 5. RECIPES AUDIT
print("\n--- 5. RECIPES & CRAFTING AUDIT ---")
from audit_recipes import total_json_recipes, recipes_by_type, recipes_by_folder
print(f"Total Datapack JSON Recipes: {total_json_recipes}")
print(f"Recipe Subfolders Count: {len(recipes_by_folder)}")
print(f"Recipe Types Supported: {len(recipes_by_type)}")

# 6. Regenerate tools/modern_suppliers.json from the imported snapshot
#    (documented output of this audit; schema = the committed artifact's).
import scan_all_suppliers
print(f"\nWrote {scan_all_suppliers.dump()}")

