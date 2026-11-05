#!/usr/bin/env python3
"""Fetch a pinned shader pack solely for native acceptance; it is never redistributed."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('destination',type=Path)
args=parser.parse_args()
lock=json.loads(Path(__file__).with_name('audit5-shaders.lock.json').read_text())
if Path(lock['pack']).name!=lock['pack']:
    raise ValueError('Unsafe shader pack filename')
args.destination.mkdir(parents=True,exist_ok=True)
target=args.destination/lock['pack']
data=target.read_bytes() if target.exists() else urllib.request.urlopen(lock['url'],timeout=120).read()
if hashlib.sha512(data).hexdigest()!=lock['sha512']:
    raise ValueError('Shader acceptance dependency checksum mismatch')
if not target.exists():target.write_bytes(data)
print('Verified test-only shader pack:',target.name)
