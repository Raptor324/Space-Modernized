"""Pflegt src/main/java/com/space_m/datagen/SpaceLang.java: Uebersetzungen 1:1 aus en_US.lang / ru_RU.lang
von NTM Space (Hbm-s-Nuclear-Tech-GIT-space-travel-twopointfive). Gegenstueck zu HBM tools/restport_lang.py.

Aufruf: python tools/space_lang.py ARG [ARG ...]
  portKey=origKey        z.B. block.space_m.moon_turf=tile.moon_turf.name
  portKey=!English|Russ  direkter Text (nur wenn das Original keinen Schluessel hat; im Bericht begruenden)
  b:<id>[=<origId>]      Kurzform Block:  block.space_m.<id> = tile.<origId|id>.name
  i:<id>[=<origId>]      Kurzform Item:   item.space_m.<id>  = item.<origId|id>.name
  e:<id>[=<origName>]    Kurzform Entity: entity.space_m.<id> = entity.<origName|id>.name
  -portKey               Eintrag entfernen
  --list                 vorhandene Eintraege ausgeben
  --unported [prefix]    Space-neue Originalschluessel ohne Eintrag (optional nur mit Praefix)

Bereits vorhandene portKeys werden ersetzt. Fehlt der Schluessel in ru_RU, wird en_US genommen.
Der Originalschluessel wird mitgespeichert (space_gap.py zaehlt damit die portierten Lang-Schluessel).
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
BASE = os.path.dirname(os.path.dirname(ROOT))  # .../VS Projekte HBM Modernized
ORIG = os.path.join(BASE, 'Hbm-s-Nuclear-Tech-GIT-space-travel-twopointfive', 'src', 'main', 'resources', 'assets', 'hbm', 'lang')
MASTER = os.path.join(BASE, 'Hbm-s-Nuclear-Tech-GIT-master', 'Hbm-s-Nuclear-Tech-GIT-master', 'src', 'main', 'resources', 'assets', 'hbm', 'lang')
JAVA = os.path.join(ROOT, 'src', 'main', 'java', 'com', 'space_m', 'datagen', 'SpaceLang.java')
BS = chr(92)
PAT = re.compile(r'e\("((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)", "((?:[^"\\]|\\.)*)"\);')


def load(folder, name):
    d = {}
    p = os.path.join(folder, name)
    if not os.path.exists(p):
        return d
    with open(p, encoding='utf-8', errors='ignore') as f:
        for line in f:
            line = line.rstrip('\n').rstrip('\r')
            if '=' in line and not line.lstrip().startswith('#'):
                k, v = line.split('=', 1)
                d[k.strip()] = v
    return d


def jesc(s):
    return s.replace(BS, BS * 2).replace('"', BS + '"')


def read_entries():
    entries = {}
    if os.path.exists(JAVA):
        for m in PAT.finditer(open(JAVA, encoding='utf-8').read()):
            entries[m.group(1)] = (m.group(2), m.group(3), m.group(4))
    return entries


def expand(arg):
    m = re.match(r'^([bie]):([a-z0-9_./]+)(?:=(.+))?$', arg)
    if not m:
        return arg
    kind, pid, oid = m.group(1), m.group(2), m.group(3) or m.group(2)
    if kind == 'b':
        return 'block.space_m.%s=tile.%s.name' % (pid, oid)
    if kind == 'i':
        return 'item.space_m.%s=item.%s.name' % (pid, oid)
    return 'entity.space_m.%s=entity.%s.name' % (pid, oid)


def write(entries):
    lines = ['        e("%s", "%s", "%s", "%s");' % (k, v[0], v[1], v[2]) for k, v in sorted(entries.items())]
    head = (
        '//? if forge {\n'
        'package com.space_m.datagen;\n\n'
        'import java.util.function.BiConsumer;\n\n'
        '/**\n'
        ' * Uebersetzungen des Addons, 1:1 aus {@code en_US.lang}/{@code ru_RU.lang} von NTM Space.\n'
        ' * Wird von {@code tools/space_lang.py} gepflegt - nicht von Hand editieren.\n'
        ' * Format: {@code e(portKey, origKey, englisch, russisch)}; origKey "!" = direkter Text ohne Originalschluessel.\n'
        ' */\n'
        'public final class SpaceLang {\n\n'
        '    private SpaceLang() {}\n\n'
        '    private static BiConsumer<String, String> out;\n'
        '    private static boolean russian;\n\n'
        '    private static void e(String key, String orig, String en, String ru) {\n'
        '        out.accept(key, russian ? ru : en);\n'
        '    }\n\n'
        '    public static void addAll(BiConsumer<String, String> consumer, boolean ru) {\n'
        '        out = consumer;\n'
        '        russian = ru;\n')
    tail = '    }\n}\n//?}\n'
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    with open(JAVA, 'w', encoding='utf-8', newline='\n') as f:
        f.write(head + ('\n'.join(lines) + '\n' if lines else '') + tail)


def main(args):
    en, ru = load(ORIG, 'en_US.lang'), load(ORIG, 'ru_RU.lang')
    entries = read_entries()
    if '--list' in args:
        for k, v in sorted(entries.items()):
            print(k, '<-', v[0], '=', v[1])
        return
    if '--unported' in args:
        i = args.index('--unported')
        prefix = args[i + 1] if i + 1 < len(args) else ''
        base = load(MASTER, 'en_US.lang')
        done = {v[0] for v in entries.values()}
        todo = [k for k in en if k not in base and k not in done and k.startswith(prefix)]
        for k in todo:
            print(k, '=', en[k])
        print(len(todo), 'offen', file=sys.stderr)
        return
    missing = []
    for raw in args:
        if raw.startswith('-') and not raw.startswith('--'):
            entries.pop(raw[1:], None)
            continue
        p = expand(raw)
        if '=' not in p:
            print('Unbekanntes Argument:', raw)
            continue
        pk, ok = p.split('=', 1)
        if ok.startswith('!'):
            t = ok[1:].split('|')
            entries[pk] = ('!', jesc(t[0]), jesc(t[1] if len(t) > 1 else t[0]))
            continue
        if ok not in en:
            missing.append(ok)
            continue
        entries[pk] = (jesc(ok), jesc(en[ok]), jesc(ru.get(ok, en[ok])))
    write(entries)
    if missing:
        print('FEHLT im Original:', ' '.join(missing))
    print(len(entries), 'Eintraege')


def _locked(fn, *a):
    # Sperre gegen parallele Laeufe (mehrere Agenten schreiben dieselbe SpaceLang.java)
    import time
    lock = JAVA + '.lock'
    os.makedirs(os.path.dirname(JAVA), exist_ok=True)
    for _ in range(600):
        try:
            os.mkdir(lock)
            break
        except FileExistsError:
            time.sleep(0.5)
    try:
        return fn(*a)
    finally:
        try:
            os.rmdir(lock)
        except OSError:
            pass


if __name__ == '__main__':
    _locked(main, sys.argv[1:])
