"""Готовит tools/translation_fix/tabnuke_manifest.json: все предметы вкладки tabNuke
HBM-Modernized (109 канонических из canonical_tab_aliases.json + 3 space-блока из SpaceBombs)
с текущими en/ru из сгенерированных lang и оригинальными en/ru из 1.7.10.

Критерий (по запросу): полное соответствие оригиналу для ОБЕИХ локализаций.
Урок book_guide: префикс в orig_id алиаса может быть неверным (tile vs item) — пробуем оба;
match засчитывается при совпадении с ЛЮБЫМ кандидатом (value-lookup может дать устаревшее имя).
"""
import json
import os

CORE = 'C:/Projects/HBM-Modernized-agents'
SPACE = 'C:/Projects/Space-Modernized'
FORK = 'C:/Projects/NTM-Space-1.7.10/src/main/resources/assets/hbm/lang'
BASE = 'C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/resources/assets/hbm/lang'
ALIAS = CORE + '/tools/tab_sync/canonical_tab_aliases.json'
CORE_EN = CORE + '/src/generated/resources/assets/hbm_m/lang/en_us.json'
CORE_RU = CORE + '/src/generated/resources/assets/hbm_m/lang/ru_ru.json'
SPACE_EN = SPACE + '/src/generated/resources/assets/space_m/lang/en_us.json'
SPACE_RU = SPACE + '/src/generated/resources/assets/space_m/lang/ru_ru.json'
OUT = SPACE + '/tools/translation_fix/tabnuke_manifest.json'


def load_pair(folder):
    en = {}
    ru = {}
    with open(os.path.join(folder, 'en_US.lang'), encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.rstrip('\r\n')
            if '=' in line and not line.lstrip().startswith('#'):
                k, v = line.split('=', 1)
                en[k.strip()] = v
    with open(os.path.join(folder, 'ru_RU.lang'), encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.rstrip('\r\n')
            if '=' in line and not line.lstrip().startswith('#'):
                k, v = line.split('=', 1)
                ru[k.strip()] = v
    return en, ru


def val_index(en):
    idx = {}
    for k, v in en.items():
        idx.setdefault(v, []).append(k)
    return idx


base_en, base_ru = load_pair(BASE)
fork_en, fork_ru = load_pair(FORK)
base_by_val = val_index(base_en)
fork_by_val = val_index(fork_en)
core_en = json.load(open(CORE_EN, encoding='utf-8'))
core_ru = json.load(open(CORE_RU, encoding='utf-8'))
space_en = json.load(open(SPACE_EN, encoding='utf-8'))
space_ru = json.load(open(SPACE_RU, encoding='utf-8'))

alias = json.load(open(ALIAS, encoding='utf-8'))
items = alias['tabs']['tabNuke']['items']


def orig_candidates(orig_id, name):
    """Кандидаты origKey: ключ из orig_id (оба префикса tile/item) + поиск по значению (meta-варианты)."""
    out = []
    seen = set()

    def add(repo, ok):
        if not ok or ok in seen:
            return
        lang_en, lang_ru = (base_en, base_ru) if repo == 'base' else (fork_en, fork_ru)
        en = lang_en.get(ok)
        if en is None:
            return
        seen.add(ok)
        out.append({'repo': repo, 'origKey': ok, 'en': en, 'ru': lang_ru.get(ok, en),
                    'ruFound': ok in lang_ru})

    name_part = None
    for pref in ('tile', 'item', 'entity'):
        if orig_id.startswith('hbm:%s.' % pref):
            name_part = orig_id[len('hbm:%s.' % pref):].split('/')[0]
            break
    if name_part:
        for pref in ('item', 'tile'):
            for repo in ('base', 'fork'):
                add(repo, '%s.%s.name' % (pref, name_part))
    if name:
        for repo, by_val in (('base', base_by_val), ('fork', fork_by_val)):
            for ok in by_val.get(name, [])[:2]:
                lang_en = base_en if repo == 'base' else fork_en
                if ok in seen:
                    continue
                lang_ru = base_ru if repo == 'base' else fork_ru
                seen.add(ok)
                out.append({'repo': repo, 'origKey': ok, 'en': lang_en[ok],
                            'ru': lang_ru.get(ok, lang_en[ok]), 'ruFound': ok in lang_ru})
    return out


core_entries = []
stats = {'core_en_mismatch': 0, 'core_ru_mismatch': 0, 'core_no_modern_key': 0, 'core_no_orig': 0,
         'space_mismatch': 0, 'space_ok': 0, 'space_no_orig': 0}
for it in items:
    rid = it['modernized_registry_id']
    name = it.get('name') or ''
    ns, _, mid = rid.partition(':')
    cands = orig_candidates(it['orig_id'], name)
    modern_key = None
    for pref in ('item', 'block'):
        k = '%s.%s.%s' % (pref, ns, mid)
        if k in core_en or k in core_ru:
            modern_key = k
            break
    cur_en = core_en.get(modern_key) if modern_key else None
    cur_ru = core_ru.get(modern_key) if modern_key else None
    en_vals = {c['en'] for c in cands}
    ru_vals = {c['ru'] if c['ruFound'] else c['en'] for c in cands}
    en_match = cur_en is not None and cur_en in en_vals
    ru_match = cur_ru is not None and cur_ru in ru_vals
    if not modern_key:
        stats['core_no_modern_key'] += 1
    if not cands:
        stats['core_no_orig'] += 1
    if cands and cur_en is not None and not en_match:
        stats['core_en_mismatch'] += 1
    if cands and cur_ru is not None and not ru_match:
        stats['core_ru_mismatch'] += 1
    core_entries.append({
        'order': it['order'], 'orig_id': it['orig_id'], 'meta': it.get('meta'),
        'registry_id': rid, 'alias_name': name, 'modern_key': modern_key,
        'current_en': cur_en, 'current_ru': cur_ru,
        'en_match': en_match, 'ru_match': ru_match,
        'candidates': cands, 'tab_status': it.get('status'),
    })

# space-блоки на вкладке NUKE (SpaceBombs.java:52-61); oil_barrel без имени в оригинале
space_defs = [
    ('block.space_m.nuke_antimatter', 'tile.nuke_antimatter.name'),
    ('block.space_m.det_salt', 'tile.det_salt.name'),
    ('block.space_m.oil_barrel', 'tile.oil_barrel.name'),
]
space_entries = []
for port_key, orig_key in space_defs:
    cur_en = space_en.get(port_key)
    cur_ru = space_ru.get(port_key)
    o_en = fork_en.get(orig_key)
    o_ru = fork_ru.get(orig_key)
    has_orig = o_en is not None
    en_match = cur_en == o_en if (cur_en is not None and o_en is not None) else False
    ru_match = cur_ru == o_ru if (cur_ru is not None and o_ru is not None) else False
    if not has_orig:
        stats['space_no_orig'] += 1
    elif en_match and ru_match:
        stats['space_ok'] += 1
    else:
        stats['space_mismatch'] += 1
    space_entries.append({
        'port_key': port_key, 'orig_key': orig_key,
        'current_en': cur_en, 'current_ru': cur_ru,
        'orig_en': o_en, 'orig_ru': o_ru,
        'en_match': en_match, 'ru_match': ru_match, 'has_orig': has_orig,
    })

manifest = {'stats': stats, 'core': core_entries, 'space': space_entries}
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, 'w', encoding='utf-8') as f:
    json.dump(manifest, f, ensure_ascii=False, indent=1)

print(json.dumps(stats, ensure_ascii=False))
print('--- core mismatches ---')
for e in core_entries:
    if not e['en_match'] or not e['ru_match']:
        print('%3d %s | key=%s | en: %r | ru: %r' % (e['order'], e['registry_id'], e['modern_key'], e['current_en'], e['current_ru']))
        for c in e['candidates']:
            print('     cand[%s] %s = %r / %r' % (c['repo'], c['origKey'], c['en'], c['ru']))
print('--- space ---')
for e in space_entries:
    print(e['port_key'], '| en_match:', e['en_match'], '| ru_match:', e['ru_match'], '| has_orig:', e['has_orig'],
          '| current:', repr(e['current_en']), repr(e['current_ru']))
