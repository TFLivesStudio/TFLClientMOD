#!/usr/bin/env python3
"""Rebuild independent optimizers from pinned source; fail on unexpected artifact bytes."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import shutil
import subprocess
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--workspace', type=Path, required=True)
    args = parser.parse_args()
    args.workspace.mkdir(parents=True, exist_ok=True)
    locks = json.loads((ROOT / 'tools/integrations/optimizer-sources.lock.json').read_text())
    for name, lock in zip(('zirconium', 'starlight'), locks):
        folder = args.workspace / name
        if folder.exists():
            raise ValueError(f'Refusing to replace an existing source tree: {folder}')
        data = urllib.request.urlopen(f"https://api.github.com/repos/{lock['repo']}/zipball/{lock['revision']}", timeout=60).read()
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            for member in archive.infolist():
                parts = Path(member.filename).parts
                if member.filename.startswith('/') or '\\' in member.filename or '..' in parts or (member.external_attr >> 16) & 0o170000 == 0o120000:
                    raise ValueError('Unsafe upstream source archive member')
                if member.is_dir() or len(parts) < 2:
                    continue
                target = folder.joinpath(*parts[1:])
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(archive.read(member))
        for patch in lock.get('patches', []):
            subprocess.run(['patch', '-p1', '--batch', '-i', str(ROOT / 'tools/integrations' / patch)], cwd=folder, check=True)
        subprocess.run(['bash', 'gradlew', 'test', 'build', '--no-daemon', '--max-workers=2'], cwd=folder, check=True)
        artifact = folder / 'build/libs' / lock['jar']
        actual = hashlib.sha256(artifact.read_bytes()).hexdigest()
        if actual != lock['builtSha256']:
            raise ValueError(f"Reproducibility gate failed for {name}: expected {lock['builtSha256']}, actual {actual}")
        print(f'{name}: verified {actual}', flush=True)
    dh = json.loads((ROOT / 'tools/integrations/distant-horizons.lock.json').read_text())
    for source in dh['correspondingSources']:
        target = args.workspace / source['filename']
        data = urllib.request.urlopen(source['url'], timeout=60).read()
        if hashlib.sha256(data).hexdigest() != source['sha256']:
            raise ValueError('DH corresponding-source checksum mismatch: ' + source['filename'])
        target.write_bytes(data)


if __name__ == '__main__':
    main()
