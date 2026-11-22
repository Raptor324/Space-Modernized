"""Set of ore names registered via PlanetOres.ore("name", ...) in the
modernized repo. Restored helper (module was lost before the audit scripts
were first committed); consumed by final_parity_audit.py as::

    from count_planet_ores import ores_registered  # membership test by name

Same scan basis as the working check_planet_ores.py, but keyed on the ore
name only (the body argument is often a constant, not a literal), and
print-free so importing it stays quiet. Stdlib only; run from tools/.
"""
import re
from pathlib import Path

SPACE_JAVA = Path(r"C:\Projects\Space-Modernized\src\main\java")

_ORE_RE = re.compile(r'\bPlanetOres\.ore\s*\(\s*"([^"]+)"')

ores_registered = set()
for _p in SPACE_JAVA.rglob("*.java"):
    ores_registered.update(
        _ORE_RE.findall(_p.read_text(encoding="utf-8", errors="ignore")))
