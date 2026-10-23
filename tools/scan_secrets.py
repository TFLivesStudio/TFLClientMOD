#!/usr/bin/env python3
"""Conservative tracked-source secret check. Reports locations, never matching contents."""
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
RULE = re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{36,}|AKIA[0-9A-Z]{16}|xox[baprs]-[0-9A-Za-z-]{24,}')
failures = []
for name in subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).decode().split('\0'):
    if not name:
        continue
    p = ROOT / name
    if not p.is_file():
        continue
    try:
        content = p.read_text('utf-8')
    except (UnicodeError, OSError):
        continue
    for line, value in enumerate(content.splitlines(), 1):
        if RULE.search(value):
            failures.append(f'{name}:{line}')
if failures:
    raise SystemExit('Potential secret locations:\n' + '\n'.join(failures))
print('No private-key, GitHub-token, AWS-access-key or Slack-token patterns found in tracked text.')
