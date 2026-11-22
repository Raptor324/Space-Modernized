import os
import re
from pathlib import Path
from scan_registrations import scan_registrations

NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")
SPACE_MOD = Path(r"C:\Projects\Space-Modernized")
HBM_MOD = Path(r"C:\Projects\HBM-Modernized")

def read_file(p):
    return p.read_text(encoding="utf-8", errors="ignore") if p.exists() else ""

ntm_items_src = read_file(NTM_1710 / "src/main/java/com/hbm/items/ModItems.java")
git_items_src = read_file(HBM_GIT / "src/main/java/com/hbm/items/ModItems.java")

ntm_item_fields = {}
for m in re.finditer(r'public\s+static\s+(?:final\s+)?Item\s+(\w+)', ntm_items_src):
    ntm_item_fields[m.group(1)] = m.group(1)

git_item_fields = set()
for m in re.finditer(r'public\s+static\s+(?:final\s+)?Item\s+(\w+)', git_items_src):
    git_item_fields.add(m.group(1))

space_items_1710 = [f for f in ntm_item_fields if f not in git_item_fields]

item_details_1710 = {}
for item in space_items_1710:
    unloc_match = re.search(rf'\b{item}\s*=\s*(?:[^\n;]+?)\.setUnlocalizedName\("([^"]+)"\)', ntm_items_src)
    reg_match = re.search(rf'GameRegistry\.registerItem\(\s*{item}\s*,\s*"([^"]+)"\)', ntm_items_src)
    class_match = re.search(rf'\b{item}\s*=\s*new\s+([A-Za-z0-9_]+)\b', ntm_items_src)
    
    unloc = unloc_match.group(1) if unloc_match else None
    reg = reg_match.group(1) if reg_match else None
    cls = class_match.group(1) if class_match else None
    
    item_details_1710[item] = {
        'field': item,
        'unloc': unloc or item,
        'reg': reg or unloc or item,
        'class': cls
    }

blocks, modern_items, bes, fluids, entities, menus = scan_registrations()

# Also scan HBM-Modernized for items
hbm_mod_items = set()
hbm_mod_java = HBM_MOD / "src/main/java"
for root, dirs, files in os.walk(hbm_mod_java):
    for f in files:
        if f.endswith(".java") and ("Item" in f or "ModItems" in f):
            c = (Path(root) / f).read_text(encoding="utf-8", errors="ignore")
            for m in re.finditer(r'(?:registerItem|basic|register)\s*\(\s*"([^"]+)"', c):
                hbm_mod_items.add(m.group(1))

print("=== ITEM PARITY AUDIT ===")
print(f"1.7.10 Space Items Count: {len(item_details_1710)}")
print(f"Space-Modernized Items Registered: {len(modern_items)}")

matched_items = []
in_hbm_base_items = []
missing_items = []

for item, d in sorted(item_details_1710.items()):
    unloc = d['unloc']
    reg = d['reg']
    found = None
    for cand in [unloc, reg, item, unloc.lower(), reg.lower(), item.lower()]:
        if cand in modern_items:
            found = modern_items[cand]
            break
    if found:
        matched_items.append((d, found))
    else:
        hbm_found = None
        for cand in [unloc, reg, item, unloc.lower(), reg.lower(), item.lower()]:
            if cand in hbm_mod_items:
                hbm_found = cand
                break
        if hbm_found:
            in_hbm_base_items.append((d, hbm_found))
        else:
            missing_items.append(d)

print(f"\nPorted in Space-Modernized: {len(matched_items)} / {len(item_details_1710)} ({len(matched_items)/len(item_details_1710)*100:.1f}%)")
print(f"Present in Base HBM-Modernized: {len(in_hbm_base_items)}")
print(f"Completely Missing: {len(missing_items)}")

if in_hbm_base_items:
    print("\n--- Items Found in Base HBM-Modernized ---")
    for d, hid in in_hbm_base_items:
        print(f"  {d['field']} (unloc={d['unloc']}) -> {hid}")

print("\n--- Missing Items ---")
for d in missing_items:
    print(f"  {d['field']} (unloc={d['unloc']}, class={d['class']})")

print(f"\nExtra Items registered in Space-Modernized: {len(modern_items) - len(matched_items)}")
