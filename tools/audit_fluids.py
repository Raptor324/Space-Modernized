import os
import re
from pathlib import Path

SPACE_JAVA = Path(r"C:\Projects\Space-Modernized\src\main\java")
NTM_1710 = Path(r"C:\Projects\NTM-Space-1.7.10")
HBM_GIT = Path(r"C:\Projects\Hbm-s-Nuclear-Tech-GIT")

c = (SPACE_JAVA / "com/space_m/fluid/SpaceFluids.java").read_text(encoding="utf-8")
# match gas("id", ...), liquid("id", ...), heavy("id", ...), plasma("id", ...)
fluids = re.findall(r'(gas|liquid|heavy|plasma)\s*\(\s*"([^"]+)"', c)
print(f"Total fluids registered in SpaceFluids.java: {len(fluids)}")

# 1.7.10 Fluids audit
f_1710 = (NTM_1710 / "src/main/java/com/hbm/inventory/fluid/Fluids.java").read_text(encoding="utf-8", errors="ignore")
f_git = (HBM_GIT / "src/main/java/com/hbm/inventory/fluid/Fluids.java").read_text(encoding="utf-8", errors="ignore")

def extract_fluids(text):
    res = {}
    for m in re.finditer(r'public\s+static\s+(?:final\s+)?FluidType\s+(\w+)', text):
        res[m.group(1)] = m.group(1)
    return res

ntm_fluids = extract_fluids(f_1710)
git_fluids = extract_fluids(f_git)

space_fluids_1710 = {k: v for k, v in ntm_fluids.items() if k not in git_fluids}
print(f"\n1.7.10 NTM Fluids: {len(ntm_fluids)}")
print(f"1.7.10 Base HBM Fluids: {len(git_fluids)}")
print(f"1.7.10 Space-added Fluids: {len(space_fluids_1710)}")

modern_names = {name.lower(): (kind, name) for kind, name in fluids}

matched = []
missing = []

for f_name in space_fluids_1710:
    name_clean = f_name.lower().replace("_", "")
    found = None
    if f_name.lower() in modern_names:
        found = modern_names[f_name.lower()]
    elif name_clean in modern_names:
        found = modern_names[name_clean]
    elif f_name.lower() == "kmno4" and "kmno4" in modern_names:
        found = modern_names["kmno4"]
    elif f_name.lower() == "earthair":
        found = ("gas", "hbm_m:air (alias)")
    
    if found:
        matched.append((f_name, found))
    else:
        # check if it was commented out in 1.7.10
        is_init = bool(re.search(rf'\b{f_name}\s*=\s*new\s+FluidType', f_1710))
        missing.append((f_name, is_init))

print(f"\nPorted in SpaceFluids: {len(matched)} / {len(space_fluids_1710)} ({len(matched)/len(space_fluids_1710)*100:.1f}%)")
print(f"Missing or Not Ported: {len(missing)}")
for m, is_init in missing:
    print(f"  {m:<30} (initialized in 1.7.10: {is_init})")

print("\n--- Ported Fluids Breakdown ---")
for k in ["liquid", "gas", "heavy", "plasma"]:
    group = [n for kind, n in fluids if kind == k]
    print(f"  {k.upper()}: {len(group)} -> {', '.join(sorted(group))}")
