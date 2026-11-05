#!/usr/bin/env python3
"""Create deterministic corresponding-source assets; no upload or public deployment.

Optimizer source directories must be the reviewed pinned trees after applying the
patches listed in optimizer-sources.lock.json. DH archives are verified by SHA256,
including its coreSubProjects gitlink. Core source always comes from Git HEAD.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
EXCLUDE = {'.git', '.gradle', 'build', 'run', 'out', '.idea', '__pycache__'}


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def entry(archive, name, data):
    info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    info.external_attr = 0o100644 << 16
    archive.writestr(info, data)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--upstream', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT / 'tools/integrations/optimizer-sources.lock.json').read_text())
    assets = {}
    for name, source in zip(('zirconium', 'starlight'), lock):
        folder = args.upstream / name
        if not (folder / 'LICENSE').is_file():
            raise ValueError(f'Missing reviewed {name} source/license tree')
        target = args.output / f'{name}-corresponding-source.zip'
        with zipfile.ZipFile(target, 'w') as archive:
            for path in sorted(folder.rglob('*')):
                relative = path.relative_to(folder)
                if path.is_file() and not path.is_symlink() and not EXCLUDE.intersection(relative.parts):
                    entry(archive, 'source/' + relative.as_posix(), path.read_bytes())
            entry(archive, 'COPYING-GPL-3.0.txt', (ROOT / 'vendor/trs-client/LICENSE').read_bytes())
            entry(archive, 'TFL-PROVENANCE.json', (json.dumps(source, indent=2) + '\n').encode())
            for patch in source.get('patches', []):
                entry(archive, 'applied-' + Path(patch).name, (ROOT / 'tools/integrations' / patch).read_bytes())
            entry(archive, 'BUILD.txt', b'Corresponding source for the independent optimizer.\nBase revision and applied patches: TFL-PROVENANCE.json.\nThe source/ tree is already patched: do not apply patches twice.\nUse Java 25, then cd source and run: bash gradlew test build --no-daemon --max-workers=2\nOn Windows use gradlew.bat test build.\nGPL/LGPL notices are preserved in source/LICENSE and source files.\n')
        assets[name] = {'assetName': target.name, 'sha256': sha(target), 'bytes': target.stat().st_size}
    dh = json.loads((ROOT / 'tools/integrations/distant-horizons.lock.json').read_text())
    target = args.output / 'tflcore-corresponding-source.zip'
    commit = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    core = subprocess.check_output(['git', 'archive', '--format=tar', 'HEAD'], cwd=ROOT)
    with zipfile.ZipFile(target, 'w') as archive:
        entry(archive, 'tflcore-source.tar', core)
        for source in dh['correspondingSources']:
            path = args.upstream / source['filename']
            if sha(path) != source['sha256']:
                raise ValueError(f'DH corresponding-source checksum mismatch: {path.name}')
            entry(archive, 'distant-horizons/' + path.name, path.read_bytes())
        entry(archive, 'PROVENANCE.json', json.dumps({'coreCommit': commit, 'distantHorizons': dh}, indent=2).encode())
        entry(archive, 'BUILD.txt', b'Extract tflcore-source.tar. Core build: Java 25; see AGENTS.md and the committed Gradle wrappers.\nDH is the original unmodified nested Fabric artifact, not relocated.\nIts full sources include both pinned GitLab archives: extract the main archive,\nthen extract the core archive into its coreSubProjects directory (strip archive root).\nSee PROVENANCE.json for revisions, SHA256, upstream build scripts and LGPL notices.\nNo signing keys or user authentication credentials are required to compile Core.\n')
    assets['core'] = {'assetName': target.name, 'sha256': sha(target), 'bytes': target.stat().st_size, 'commit': commit}
    (args.output / 'source-assets.json').write_text(json.dumps(assets, indent=2) + '\n')
    print(json.dumps(assets, indent=2))


if __name__ == '__main__':
    main()
