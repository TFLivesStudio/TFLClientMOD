#!/usr/bin/env python3
"""The clients can exit normally after a failed assertion; always inspect their actual report."""
import json,pathlib,sys
p=pathlib.Path(sys.argv[1]);r=json.loads(p.read_text());checks=r.get('checks',r.get('results',[]))
assert checks and all(c['passed'] for c in checks), {'report':str(p),'error':r.get('error'),'failed':[c for c in checks if not c['passed']]}
assert r.get('status','passed')=='passed',r.get('error')
print(f'Verified {p.name}: {len(checks)} native checks')
