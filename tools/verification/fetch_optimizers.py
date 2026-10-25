#!/usr/bin/env python3
"""Download pinned, hash-verified local test dependencies; no redistribution bundle."""
import argparse,hashlib,json,pathlib,urllib.request
p=argparse.ArgumentParser();p.add_argument('destination',type=pathlib.Path);p.add_argument('--iris',action='store_true');a=p.parse_args()
manifest=pathlib.Path(__file__).resolve().parents[2]/'docs/evidence/depth-polish/optimizer-dependencies-263.json'
a.destination.mkdir(parents=True,exist_ok=True)
for dep in json.loads(manifest.read_text()):
 if dep['project']=='iris' and not a.iris:continue
 target=a.destination/dep['filename'];data=target.read_bytes() if target.exists() else urllib.request.urlopen(dep['url'],timeout=120).read()
 if hashlib.sha512(data).hexdigest()!=dep['sha512']:raise SystemExit('Hash mismatch: '+target.name)
 if not target.exists():target.write_bytes(data)
 print('Verified '+dep['project']+' '+dep['version']+' for local tests')
