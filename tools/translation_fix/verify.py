"""Детерминированный гейт: полное соответствие переводов оригиналам 1.7.10.

Проверяет:
- SpaceLang.java — ВЕСЬ таблица (origKey != "!"): en/ru должны совпадать 1:1 со значениями
  en_US.lang/ru_RU.lang оригиналов (форк NTM-Space-1.7.10, резерв — базовый HBM);
- RestportLang.java — ключи из manifest.json: ru совпадает с ru любого кандидата
  (или с en кандидата, если у кандидата нет русского), en — с en кандидата
  или enFallback (section=2, текущее английское сохраняется);
- присутствие: каждый portKey манифеста должен быть в SpaceLang.java или RestportLang.java.

Расхождения по ключам ВНЕ манифеста — предупреждения (гейт не валят), по ключам
манифеста — ошибки (exit 1).
"""
import json
import os
import re
import sys

SPACE = 'C:/Projects/Space-Modernized'
FORK = 'C:/Projects/NTM-Space-1.7.10/src/main/resources/assets/hbm/lang'
BASE = 'C:/Projects/Hbm-s-Nuclear-Tech-GIT/src/main/resources/assets/hbm/lang'
MANIFEST = SPACE + '/tools/translation_fix/manifest.json'
SL = SPACE + '/src/main/java/com/space_m/datagen/SpaceLang.java'
RL = 'C:/Projects/HBM-Modernized-agents/src/main/java/com/hbm_m/datagen/RestportLang.java'


def load(folder):
    en, ru = {}, {}
    for name, d in (('en_US.lang', en), ('ru_RU.lang', ru)):
        with open(os.path.join(folder, name), encoding='utf-8', errors='ignore') as f:
            for line in f:
                line = line.rstrip('\r\n')
                if '=' in line and not line.lstrip().startswith('#'):
                    k, v = line.split('=', 1)
                    d[k.strip()] = v
    return en, ru


def unesc(s):
    # разворачивает Java-экранирование \" и \\ из таблиц
    return re.sub(r'\\(.)', r'\1', s)


fork_en, fork_ru = load(FORK)
base_en, base_ru = load(BASE)
manifest = json.load(open(MANIFEST, encoding='utf-8'))
entries = manifest['entries']
by_key = {e['key']: e for e in entries}
universe = set(by_key)

errors, warnings = [], []

# --- SpaceLang: полная таблица
pat4 = re.compile(r'e\("((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\);')
sl_keys = set()
for m in pat4.finditer(open(SL, encoding='utf-8').read()):
    pk, ok = m.group(1), m.group(2)
    en, ru = unesc(m.group(3)), unesc(m.group(4))
    sl_keys.add(pk)
    if ok == '!':
        continue
    src_en, src_ru = (fork_en, fork_ru) if ok in fork_en else (base_en, base_ru)
    if ok not in src_en:
        msg = '%s: origKey %s отсутствует в оригиналах' % (pk, ok)
        (errors if pk in universe else warnings).append(msg)
        continue
    exp_en = src_en[ok]
    exp_ru = src_ru.get(ok, exp_en)
    if en != exp_en:
        msg = 'EN %s: в таблице %r, в оригинале %r' % (pk, en, exp_en)
        (errors if pk in universe else warnings).append(msg)
    if ru != exp_ru:
        msg = 'RU %s: в таблице %r, в оригинале %r' % (pk, ru, exp_ru)
        (errors if pk in universe else warnings).append(msg)

# --- RestportLang: только ключи манифеста
rl_keys = set()
pat3 = re.compile(r'e\("((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\);')
for m in pat3.finditer(open(RL, encoding='utf-8').read()):
    pk = m.group(1)
    en, ru = unesc(m.group(2)), unesc(m.group(3))
    rl_keys.add(pk)
    e = by_key.get(pk)
    if e is None:
        continue
    cands = e['candidates']
    if not cands:
        continue  # разрешённые допроверкой origKey не попали в манифест — проверяется присутствием
    ru_vals = {c['ru'] if c.get('ruFound') else c['en'] for c in cands}
    en_vals = {c['en'] for c in cands}
    if e['section'] == 2 and e.get('enFallback'):
        en_vals.add(e['enFallback'])
    if ru not in ru_vals:
        errors.append('RU(core) %s: %r не совпадает ни с одним кандидатом %r' % (pk, ru, sorted(ru_vals)[:4]))
    if en not in en_vals:
        errors.append('EN(core) %s: %r не совпадает ни с одним кандидатом %r' % (pk, en, sorted(en_vals)[:4]))

# --- присутствие portKey из манифеста
for e in entries:
    if e['key'] not in sl_keys and e['key'] not in rl_keys:
        errors.append('ОТСУТСТВУЕТ %s (%s)' % (e['key'], e['id']))

print('SpaceLang ключей: %d, RestportLang ключей: %d, записей в манифесте: %d' % (len(sl_keys), len(rl_keys), len(entries)))
if warnings:
    print('ПРЕДУПРЕЖДЕНИЯ (вне области задачи): %d' % len(warnings))
    for w in warnings[:40]:
        print('  ~ ' + w)
if errors:
    print('ОШИБКИ: %d' % len(errors))
    for x in errors[:80]:
        print('  ! ' + x)
    sys.exit(1)
print('OK: полное соответствие оригиналу в области задачи')
