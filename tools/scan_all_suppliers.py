"""Snapshot of everything the modernized repo registers, built on
scan_registrations. Restored helper (module was lost before the audit scripts
were first committed).

final_parity_audit.py imports the module-level names::

    from scan_all_suppliers import blocks, items, bes, fluids, menus

`bes` is a dict id -> {"id", "type", "file"}; the rest are dicts id -> file.
Running this module directly (or final_parity_audit.py) rewrites
tools/modern_suppliers.json — its schema (blocks, items, block_entities,
fluids, entities, menus as id lists) is taken from the last known artifact.
Stdlib only; run from tools/.
"""
import json
from pathlib import Path

from scan_registrations import scan_registrations

blocks, items, bes, fluids, entities, menus = scan_registrations()

OUT_FILE = Path(__file__).resolve().parent / "modern_suppliers.json"


def dump(path=None):
    """Write the suppliers snapshot as JSON; returns the written path."""
    target = Path(path) if path is not None else OUT_FILE
    data = {
        "blocks": list(blocks),
        "items": list(items),
        "block_entities": list(bes),
        "fluids": list(fluids),
        "entities": list(entities),
        "menus": list(menus),
    }
    target.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    return target


if __name__ == "__main__":
    print(f"blocks={len(blocks)} items={len(items)} block_entities={len(bes)} "
          f"fluids={len(fluids)} entities={len(entities)} menus={len(menus)}")
    print(f"wrote {dump()}")
