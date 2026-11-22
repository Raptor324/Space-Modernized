"""Готовит tools/translation_fix/manifest.json для workflow перевода missing_translations_ru_ru.txt.

Что делает:
- парсит versions/1.21.1-neoforge/run/missing_translations_ru_ru.txt (2 секции);
- бедроковые руды исключает: их имена компонуются в рантайме из компонент-ключей
  (ItemCelestialBedrockOre.getName / ItemBedrockOreGraded.getName), per-item ключи
  никогда не запрашиваются — это артефакт дампа ItemCopyDevHook;
- для остальных записей ищет кандидатов origKey:
  * canonical_tab_aliases.json (точное совпадение registry id + перестановка форм ingot_X <-> X_ingot);
  * прямые ключи item.<id>.name / tile.<id>.name в обоих оригиналах;
  * поиск ключа по английскому значению (для meta-вариантов, напр. oil_tar_coal);
- пишет manifest.json (записи + кандидаты с en/ru из оригиналов) и space_aliases.json
  (алиасы space-аддона: portKey -> origKey из SpaceLang.java).
"""
import json
import os
import re

SPACE = 'C:/Projects/Space-Modernized'
FORK = 'C:/Projects/NTM-Space-1.7.10/src/main/resources/assets/hbm/lang'
BASE = 'C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/resources/assets/hbm/lang'
ALIAS = 'C:/Projects/HBM-Modernized-agents/tools/tab_sync/canonical_tab_aliases.json'
REPORT = os.path.join(SPACE, 'versions', '1.21.1-neoforge', 'run', 'missing_translations_ru_ru.txt')
OUT_DIR = os.path.join(SPACE, 'tools', 'translation_fix')


def load_lang(folder):
    out = {}
    for name in ('en_US.lang', 'ru_RU.lang'):
        d = {}
        with open(os.path.join(folder, name), encoding='utf-8', errors='ignore') as f:
            for line in f:
                line = line.rstrip('\r\n')
                if '=' in line and not line.lstrip().startswith('#'):
                    k, v = line.split('=', 1)
                    d[k.strip()] = v
        out[name] = d
    return out


def val_index(en):
    idx = {}
    for k, v in en.items():
        idx.setdefault(v, []).append(k)
    return idx


fork = load_lang(FORK)
base = load_lang(BASE)
fork_by_val = val_index(fork['en_US.lang'])
base_by_val = val_index(base['en_US.lang'])

# Индекс алиасов: registry id -> orig_id; "токены имени" -> registry id (для перестановок форм)
alias = json.load(open(ALIAS, encoding='utf-8'))
by_id = {}
by_tokens = {}
alias_names = {}
for tab in alias.get('tabs', {}).values():
    for it in tab.get('items', []):
        mid = it.get('modernized_registry_id')
        oid = it.get('orig_id')
        if not mid or not oid:
            continue
        by_id.setdefault(mid, oid)
        toks = ' '.join(sorted(mid.split(':', 1)[-1].split('_')))
        by_tokens.setdefault(toks, mid)
        alias_names.setdefault(mid, it.get('name', ''))


def orig_langkey(oid):
    m = re.match(r'^hbm:(item|tile|entity)\.(.+?)(?:/\d+)?$', oid)
    if m:
        return '%s.%s.name' % (m.group(1), m.group(2))
    return None


def candidates_for(rid):
    ns, _, name = rid.partition(':')
    out = []
    seen = set()

    def add(repo, ok):
        if not ok or ok in seen or len(out) >= 6:
            return
        lang = fork if repo == 'fork' else base
        en = lang['en_US.lang'].get(ok)
        if en is None:
            return
        seen.add(ok)
        out.append({
            'repo': repo,
            'origKey': ok,
            'en': en,
            'ru': lang['ru_RU.lang'].get(ok, en),
            'ruFound': ok in lang['ru_RU.lang'],
        })

    oid = by_id.get(rid)
    if oid:
        lk = orig_langkey(oid)
        if lk:
            for repo in ('base', 'fork'):
                add(repo, lk)
        # meta-варианты: базовый orig_id даёт не тот ключ — ищем ключ по английскому имени из алиаса
        aname = alias_names.get(rid, '')
        if aname:
            for repo, idx, lang in (('base', base_by_val, base), ('fork', fork_by_val, fork)):
                for ok in idx.get(aname, [])[:2]:
                    en = lang['en_US.lang'].get(ok)
                    if en is None:
                        continue
                    out.append({
                        'repo': repo,
                        'origKey': ok,
                        'en': en,
                        'ru': lang['ru_RU.lang'].get(ok, en),
                        'ruFound': ok in lang['ru_RU.lang'],
                    })
                    seen.add(ok)

    toks = ' '.join(sorted(name.split('_')))
    mid2 = by_tokens.get(toks)
    if mid2 and mid2 != rid:
        oid2 = by_id.get(mid2)
        if oid2:
            lk2 = orig_langkey(oid2)
            if lk2:
                for repo in ('base', 'fork'):
                    add(repo, lk2)

    for pref in ('item', 'tile'):
        for repo in (('fork', 'base') if ns == 'space_m' else ('base', 'fork')):
            add(repo, '%s.%s.name' % (pref, name))
    return out


entries = []
section = 0
stats = {'bedrock': {'section1': 0, 'section2': 0}, 'other': {'section1': 0, 'section2': 0}, 'unresolved': 0}
for raw in open(REPORT, encoding='utf-8'):
    raw = raw.rstrip('\n')
    if raw.startswith('==='):
        section += 1
        continue
    if not raw.strip():
        continue
    parts = [p.strip() for p in raw.split('|')]
    key, rid = parts[0], parts[1]
    enfb = parts[2] if len(parts) > 2 else ''
    if 'bedrock_ore' in rid:
        stats['bedrock']['section%d' % section] += 1
        continue
    cands = candidates_for(rid)
    if not cands:
        stats['unresolved'] += 1
    stats['other']['section%d' % section] += 1
    entries.append({'key': key, 'id': rid, 'section': section, 'enFallback': enfb, 'candidates': cands})

os.makedirs(OUT_DIR, exist_ok=True)
with open(os.path.join(OUT_DIR, 'manifest.json'), 'w', encoding='utf-8') as f:
    json.dump({'stats': stats, 'entries': entries}, f, ensure_ascii=False, indent=1)

# Алиасы space-аддона (portKey -> origKey/en/ru) из SpaceLang.java — отдельный json, как просил пользователь
pat = re.compile(r'e\("((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\);')
al = {}
sl_path = os.path.join(SPACE, 'src', 'main', 'java', 'com', 'space_m', 'datagen', 'SpaceLang.java')
for m in pat.finditer(open(sl_path, encoding='utf-8').read()):
    al[m.group(1)] = {'origKey': m.group(2), 'en': m.group(3), 'ru': m.group(4)}
with open(os.path.join(OUT_DIR, 'space_aliases.json'), 'w', encoding='utf-8') as f:
    json.dump(al, f, ensure_ascii=False, indent=1, sort_keys=True)

print(json.dumps(stats, ensure_ascii=False))
print('non-bedrock entries:', len(entries), '| space_aliases:', len(al))
print('--- unresolved (первые 40) ---')
n = 0
for e in entries:
    if not e['candidates']:
        n += 1
        if n <= 40:
            print('  ?', e['key'], '|', e['id'])
