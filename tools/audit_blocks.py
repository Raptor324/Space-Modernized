import os
import re
from pathlib import Path
from scan_registrations import scan_registrations

NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")
SPACE_MOD = Path(r"C:\Projects\Space-Modernized")
HBM_MOD = Path(r"C:\Projects\HBM-Modernized")

# 1. Parse all blocks in 1.7.10 ModBlocks
def read_file(p):
    return p.read_text(encoding="utf-8", errors="ignore") if p.exists() else ""

ntm_blocks_src = read_file(NTM_1710 / "src/main/java/com/hbm/blocks/ModBlocks.java")
git_blocks_src = read_file(HBM_GIT / "src/main/java/com/hbm/blocks/ModBlocks.java")

# Extract fields
ntm_fields = {}
for m in re.finditer(r'public\s+static\s+(?:final\s+)?Block\s+(\w+)', ntm_blocks_src):
    ntm_fields[m.group(1)] = m.group(1)

git_fields = set()
for m in re.finditer(r'public\s+static\s+(?:final\s+)?Block\s+(\w+)', git_blocks_src):
    git_fields.add(m.group(1))

space_blocks_1710 = [f for f in ntm_fields if f not in git_fields]

# Extract details for each space block in 1.7.10
block_details_1710 = {}
for b in space_blocks_1710:
    # unlocalized name
    unloc_match = re.search(rf'\b{b}\s*=\s*(?:[^\n;]+?)\.setBlockName\("([^"]+)"\)', ntm_blocks_src)
    reg_match = re.search(rf'GameRegistry\.registerBlock\(\s*{b}\s*,\s*"([^"]+)"\)', ntm_blocks_src)
    if not reg_match:
        reg_match = re.search(rf'GameRegistry\.registerBlock\(\s*{b}\s*,[^,]*"([^"]+)"\)', ntm_blocks_src)
    # class type
    class_match = re.search(rf'\b{b}\s*=\s*new\s+([A-Za-z0-9_]+)\b', ntm_blocks_src)
    
    unloc = unloc_match.group(1) if unloc_match else None
    reg = reg_match.group(1) if reg_match else None
    cls = class_match.group(1) if class_match else None
    
    block_details_1710[b] = {
        'field': b,
        'unloc': unloc or b,
        'reg': reg or unloc or b,
        'class': cls
    }

# 2. Get Modernized blocks
modern_blocks, modern_items, modern_bes, modern_fluids, _, _ = scan_registrations()

# Also scan HBM-Modernized for blocks (to see if any space blocks are in HBM-Modernized)
hbm_mod_blocks = set()
hbm_mod_java = HBM_MOD / "src/main/java"
for root, dirs, files in os.walk(hbm_mod_java):
    for f in files:
        if f.endswith(".java") and "Block" in f:
            c = (Path(root) / f).read_text(encoding="utf-8", errors="ignore")
            for m in re.finditer(r'(?:registerWithItem|dropsSelf|cube|register)\s*\(\s*"([^"]+)"', c):
                hbm_mod_blocks.add(m.group(1))

print(f"=== BLOCK PARITY AUDIT ===")
print(f"1.7.10 Space Blocks Count: {len(block_details_1710)}")
print(f"Space-Modernized Blocks Count: {len(modern_blocks)}")

matched = []
in_hbm_base = []
missing = []

for b, d in sorted(block_details_1710.items()):
    unloc = d['unloc']
    reg = d['reg']
    # check candidates
    found = None
    for cand in [unloc, reg, b, unloc.lower(), reg.lower(), b.lower()]:
        if cand in modern_blocks:
            found = modern_blocks[cand]
            break
    if found:
        matched.append((d, found))
    else:
        # check if in HBM-Modernized
        hbm_found = None
        for cand in [unloc, reg, b, unloc.lower(), reg.lower(), b.lower()]:
            if cand in hbm_mod_blocks:
                hbm_found = cand
                break
        if hbm_found:
            in_hbm_base.append((d, hbm_found))
        else:
            missing.append(d)

print(f"Directly Ported in Space-Modernized: {len(matched)} / {len(block_details_1710)} ({len(matched)/len(block_details_1710)*100:.1f}%)")
print(f"Present in Base HBM-Modernized: {len(in_hbm_base)}")
print(f"Completely Missing: {len(missing)}")

print("\n--- Blocks in Base HBM-Modernized ---")
for d, hbm_id in in_hbm_base:
    print(f"  {d['field']} (unloc={d['unloc']}) -> found in HBM-Modernized as '{hbm_id}'")

print("\n--- Missing Blocks ---")
for d in missing:
    print(f"  {d['field']} (unloc={d['unloc']}, reg={d['reg']}, class={d['class']})")
