"""Scans the modernized repo's Java sources for content registrations.

Restored helper (module was lost before the audit scripts were first
committed). Consumed by audit_blocks.py, audit_items.py and compare_tiles.py
as::

    blocks, items, bes, fluids, entities, menus = scan_registrations()

Return shape inferred from those call sites and from the last known output
snapshot tools/modern_suppliers.json:
  blocks / items / fluids / entities / menus : dict id -> source file (str)
  bes                                        : dict id -> {"id", "type", "file"}
where "type" is the BlockEntity class name from the `X::new` factory argument
(compare_tiles.py substring-matches 1.7.10 tile class names against it).

Run from tools/ (hardcoded sibling-repo layout, like the other auditors).
Stdlib only.
"""
import re
from pathlib import Path

SPACE_JAVA = Path(r"C:\Projects\Space-Modernized\src\main\java")

# client/ holds renderer specs (MachineRenderers.machine("...") uses registry-
# like names without registering anything); datagen/ holds lang keys.
_SKIP_PARTS = ("client", "datagen")

# Block registrations: helper calls with a literal id first argument.
# - registerWithItem/cube : SpaceBlocks.java helpers, also used unqualified in
#   domain classes (ChemMachines etc. wrap them in own helpers)
# - machine( : ChemMachines' machine(...) block helper (client renderer specs
#   of the same name are skipped via _SKIP_PARTS)
# - crop( : SpaceCrops crop blocks; ore( : PlanetOres.ore body ore variants
# - SpaceBlocks.register : qualified direct registrations (SpaceBombs, dim blocks)
_BLOCK_PATTERNS = [
    re.compile(r'\b(?:registerWithItem|cube|ore|machine|crop)\s*\(\s*"([^"]+)"'),
    re.compile(r'\bSpaceBlocks\.register\s*\(\s*"([^"]+)"'),
]
# Bare register("...") is only a registration inside the respective registry
# class or a domain *Items.java class (SpaceRocketItems, SpaceSatelliteItems,
# SpaceMaterialItems, ... which all delegate to SpaceItems.register). The
# lookbehind skips receiver-qualified calls (BLOCKS.register, WatzPelletType.register).
_BLOCK_LOCAL = re.compile(r'(?<![\w.])register\s*\(\s*"([^"]+)"')
_ITEM_PATTERNS = [
    re.compile(r'\b(?:basic|registerItem|gen|plain|egg)\s*\(\s*"([^"]+)"'),
    re.compile(r'\bSpaceItems\.register\s*\(\s*"([^"]+)"'),
]
# Block entities: second argument is always a `Class::new` factory.
_BE_QUALIFIED = re.compile(
    r'\bSpaceBlockEntities\.register\s*\(\s*"([^"]+)"\s*,\s*([A-Za-z_][\w.]*)::new')
_BE_LOCAL = re.compile(
    r'\bregister\s*\(\s*"([^"]+)"\s*,\s*([A-Za-z_][\w.]*)::new')
# Fluids: still/flowing Fluid variants via the shared FLUIDS deferred register.
_FLUID_RE = re.compile(r'\bFLUIDS\.register\s*\(\s*"([^"]+)"')
# Entities: SpaceEntities.register(...) plus SpaceMobs' mob(...) helper.
_ENTITY_PATTERNS = [
    re.compile(r'\bSpaceEntities\.register\s*\(\s*"([^"]+)"'),
    re.compile(r'\bmob\s*\(\s*"([^"]+)"'),
]
_MENU_RE = re.compile(r'\bextended\s*\(\s*"([^"]+)"')

_REGISTRY_LOCAL_FILES = {
    "SpaceBlocks.java", "SpaceItems.java", "SpaceBlockEntities.java",
    "SpaceEntities.java", "SpaceMenus.java",
}


def _java_sources():
    for p in sorted(SPACE_JAVA.rglob("*.java")):
        if any(part in _SKIP_PARTS for part in p.parts):
            continue
        yield p, p.read_text(encoding="utf-8", errors="ignore")


def _add(table, key, value):
    if key not in table:
        table[key] = value


def _clean(key):
    """Normalize a captured id: drop 'namespace:' prefixes and trailing '_'
    left over from dynamic loops like "laythe_coral_" + variant."""
    return key.split(":")[-1].rstrip("_")


def scan_registrations():
    """Return (blocks, items, bes, fluids, entities, menus)."""
    blocks, items, bes, fluids, entities, menus = {}, {}, {}, {}, {}, {}

    for p, src in _java_sources():
        rel = str(p.relative_to(SPACE_JAVA))
        is_registry_local = p.name in _REGISTRY_LOCAL_FILES

        for pat in _BLOCK_PATTERNS:
            for m in pat.finditer(src):
                _add(blocks, _clean(m.group(1)), rel)
        for pat in _ITEM_PATTERNS:
            for m in pat.finditer(src):
                _add(items, _clean(m.group(1)), rel)
        for m in _BE_QUALIFIED.finditer(src):
            _add(bes, _clean(m.group(1)),
                 {"id": _clean(m.group(1)), "type": m.group(2), "file": rel})
        for pat in _ENTITY_PATTERNS:
            for m in pat.finditer(src):
                _add(entities, _clean(m.group(1)), rel)
        for m in _MENU_RE.finditer(src):
            _add(menus, _clean(m.group(1)), rel)
        for m in _FLUID_RE.finditer(src):
            _add(fluids, _clean(m.group(1)), rel)

        # Bare register("...") inside the registry classes and *Items.java.
        if is_registry_local or p.name.endswith("Items.java"):
            if p.name == "SpaceBlockEntities.java":
                for m in _BE_LOCAL.finditer(src):
                    _add(bes, _clean(m.group(1)),
                         {"id": _clean(m.group(1)), "type": m.group(2), "file": rel})
            elif p.name == "SpaceEntities.java":
                for m in _BLOCK_LOCAL.finditer(src):
                    _add(entities, _clean(m.group(1)), rel)
            elif p.name == "SpaceMenus.java":
                for m in _BLOCK_LOCAL.finditer(src):
                    _add(menus, _clean(m.group(1)), rel)
            elif p.name == "SpaceBlocks.java":
                for m in _BLOCK_LOCAL.finditer(src):
                    _add(blocks, _clean(m.group(1)), rel)
            else:  # SpaceItems.java and domain *Items.java -> item ids
                for m in _BLOCK_LOCAL.finditer(src):
                    _add(items, _clean(m.group(1)), rel)

    return blocks, items, bes, fluids, entities, menus


if __name__ == "__main__":
    b, i, be, f, e, m = scan_registrations()
    print(f"blocks={len(b)} items={len(i)} block_entities={len(be)} "
          f"fluids={len(f)} entities={len(e)} menus={len(m)}")
