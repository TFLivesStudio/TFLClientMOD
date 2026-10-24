#!/usr/bin/env python3
"""Download exact third-party versions to a LOCAL directory; never build a redistribution pack."""
import argparse, hashlib, json, pathlib, urllib.request
p=argparse.ArgumentParser();p.add_argument('destination',type=pathlib.Path);args=p.parse_args()
manifest=json.loads(pathlib.Path(__file__).with_name('dependencies.json').read_text())
args.destination.mkdir(parents=True,exist_ok=True)
for dep in manifest['external']:
    target=args.destination/dep['filename']
    data=target.read_bytes() if target.exists() else urllib.request.urlopen(dep['url'],timeout=120).read()
    if hashlib.sha512(data).hexdigest()!=dep['sha512']:raise SystemExit(f"Hash mismatch: {target.name}")
    if not target.exists():target.write_bytes(data)
    print(f"Verified {target.name} ({dep['license']}; local use only)")
# Fabric ignores nested library JARs on development classpaths. Expose ONLY Legacy LWJGL3's
# own bundled, open-license libraries locally; production launchers resolve nesting themselves.
import zipfile
with zipfile.ZipFile(args.destination/'legacy-lwjgl3-1.4.10.jar') as archive:
    dev=args.destination/'legacy-lwjgl3-dev-libs';dev.mkdir(exist_ok=True)
    for entry in archive.namelist():
        if entry.startswith('META-INF/jars/') and entry.endswith('.jar'):
            (dev/pathlib.PurePosixPath(entry).name).write_bytes(archive.read(entry))

# Argentum's nested runtime dependencies stay intact in this LOCAL cache (no code inspection).
with zipfile.ZipFile(args.destination/'argentum-1.0.2.jar') as archive:
    dev=args.destination/'argentum-dev-libs';dev.mkdir(exist_ok=True)
    for entry in archive.namelist():
        if entry.startswith('META-INF/jars/') and entry.endswith('.jar'):
            (dev/pathlib.PurePosixPath(entry).name).write_bytes(archive.read(entry))
