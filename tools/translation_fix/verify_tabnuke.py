"""Детерминированный гейт tabNuke: каждый предмет вкладки должен 1:1 совпадать
с оригиналом 1.7.10 в ОБОИХ локализациях (en+ru).

- core-предметы: ключ modern_key обязан присутствовать в сгенерированных
  en_us.json/ru_ru.json ядра, значения — совпадать с одним из кандидатов origKey
  (базовый HBM / форк из манифеста);
- space-предметы: присутствие в space lang + значение == форку (для has_orig);
  без оригинала — только присутствие + предупреждение.
Exit 1 при ошибках.
"""
import json
import sys

CORE = 'C:/Projects/HBM-Modernized-agents'
SPACE = 'C:/Projects/Space-Modernized'
MANIFEST = SPACE + '/tools/translation_fix/tabnuke_manifest.json'

core_en = json.load(open(CORE + '/src/generated/resources/assets/hbm_m/lang/en_us.json', encoding='utf-8'))
core_ru = json.load(open(CORE + '/src/generated/resources/assets/hbm_m/lang/ru_ru.json', encoding='utf-8'))
space_en = json.load(open(SPACE + '/src/generated/resources/assets/space_m/lang/en_us.json', encoding='utf-8'))
space_ru = json.load(open(SPACE + '/src/generated/resources/assets/space_m/lang/ru_ru.json', encoding='utf-8'))
manifest = json.load(open(MANIFEST, encoding='utf-8'))

errors, warnings = [], []

for e in manifest['core']:
    k = e['modern_key']
    if not k:
        warnings.append('нет современного ключа: %s' % e['registry_id'])
        continue
    en, ru = core_en.get(k), core_ru.get(k)
    if en is None:
        errors.append('ОТСУТСТВУЕТ EN %s' % k)
    if ru is None:
        errors.append('ОТСУТСТВУЕТ RU %s' % k)
    cands = e['candidates']
    if not cands:
        warnings.append('нет кандидатов оригинала: %s' % e['registry_id'])
        continue
    en_vals = {c['en'] for c in cands}
    ru_vals = {c['ru'] if c['ruFound'] else c['en'] for c in cands}
    if en is not None and en not in en_vals:
        errors.append('EN %s: %r не совпадает с оригиналом %r' % (k, en, sorted(en_vals)[:3]))
    if ru is not None and ru not in ru_vals:
        errors.append('RU %s: %r не совпадает с оригиналом %r' % (k, ru, sorted(ru_vals)[:3]))

for e in manifest['space']:
    k = e['port_key']
    en, ru = space_en.get(k), space_ru.get(k)
    if en is None:
        errors.append('ОТСУТСТВУЕТ EN %s' % k)
    if ru is None:
        errors.append('ОТСУТСТВУЕТ RU %s' % k)
    if e['has_orig']:
        if en != e['orig_en']:
            errors.append('EN %s: %r != оригинал %r' % (k, en, e['orig_en']))
        if ru != e['orig_ru']:
            errors.append('RU %s: %r != оригинал %r' % (k, ru, e['orig_ru']))
    else:
        warnings.append('в оригинале нет имени: %s (current=%r/%r)' % (k, en, ru))

print('core предметов: %d, space: %d' % (len(manifest['core']), len(manifest['space'])))
if warnings:
    print('ПРЕДУПРЕЖДЕНИЯ: %d' % len(warnings))
    for w in warnings:
        print('  ~ ' + w)
if errors:
    print('ОШИБКИ: %d' % len(errors))
    for x in errors[:80]:
        print('  ! ' + x)
    sys.exit(1)
print('OK: tabNuke полностью соответствует оригиналу (en+ru)')
