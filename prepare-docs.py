#!/usr/bin/env python3
from pathlib import Path
import os
import re

link_re = re.compile(r'(\[[^\]]+\])\(https://ensody.github.io/ReactiveState-Kotlin([^)]+)\)', re.U)

descr_re = re.compile(r'(?<=>)(\[\w+\])+  <br>Content  (?=<br>)')

def fix_reference(path: Path):
    content = path.read_text()\
        .replace('<br>More info  <br>', '<br><br>')
    def replace(x):
        platforms = ' '.join('`' + r.strip('[]') + '`' for r in x.groups())
        return f'Platforms: {platforms}<br>'
    path.write_text(descr_re.sub(replace, content))

def fix_guide(path: Path):
    content = path.read_text()
    def replace(x):
        if x.group(2) == "/":
            return x.group(1).strip("[]")
        return x.group(1) + '(' + x.group(2).strip('/') + '.md)'
    path.write_text(link_re.sub(replace, content))

def main():
    for root, dirs, files in os.walk(Path('docs')):
        for filename in files:
            if not filename.endswith('.md'):
                continue
            path = Path(root) / filename
            if str(path).startswith('docs/reference/') and str(path).endswith("index.md"):
                fix_reference(path)
            elif str(path).endswith("/index.md"):
                fix_guide(path)

if __name__ == '__main__':
    main()
