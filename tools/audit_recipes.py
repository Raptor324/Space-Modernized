"""Counts the datapack JSON recipes under data/space_m/recipes. Restored
helper (module was lost before the audit scripts were first committed);
consumed by final_parity_audit.py as::

    from audit_recipes import total_json_recipes, recipes_by_type, recipes_by_folder

Only the lens of the three names is printed by the consumer, so each dict
maps name -> count. Stdlib only; run from tools/.
"""
import json
from pathlib import Path

RECIPES_DIR = Path(
    r"C:\Projects\Space-Modernized\src\main\resources\data\space_m\recipes")

total_json_recipes = 0
recipes_by_folder = {}  # first folder below recipes/ (or "(root)") -> count
recipes_by_type = {}    # recipe "type" id -> count

for _p in RECIPES_DIR.rglob("*.json"):
    total_json_recipes += 1
    _rel = _p.relative_to(RECIPES_DIR)
    _folder = _rel.parent.parts[0] if _rel.parent.parts else "(root)"
    recipes_by_folder[_folder] = recipes_by_folder.get(_folder, 0) + 1
    try:
        _type = json.loads(_p.read_text(encoding="utf-8")).get("type", "unknown")
    except (json.JSONDecodeError, OSError):
        _type = "unparsable"
    recipes_by_type[_type] = recipes_by_type.get(_type, 0) + 1
