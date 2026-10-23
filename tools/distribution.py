#!/usr/bin/env python3
"""Validate the actual installable JAR and emit local build metadata. Never publish a release."""
import argparse
import hashlib
import json
import re
from pathlib import Path
import shutil
import subprocess
import zipfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ADOPTED = ROOT / 'vendor/trs-client'
CHANNELS = ('development', 'beta', 'release-candidate', 'stable')


def validate_ttf_resources(names, font):
    # Minecraft prepends font/ for TrueType providers. Bitmap providers use another resolver.
    for provider in font['providers']:
        if provider.get('type') == 'ttf':
            namespace, filename = provider['file'].split(':', 1)
            if 'assets/' + namespace + '/font/' + filename not in names:
                raise ValueError('TrueType provider points at a missing resource: ' + provider['file'])


def validate_production_references(data):
    # JVM linkage/descriptors use slash-separated names; reflective optional names use dots.
    if re.search(rb'dev/theredstonee/trsclient/dev/[A-Za-z0-9_$]+', data):
        raise ValueError('Production class links to stripped development code')


def inspect_jar(path):
    if path.name.endswith(('-sources.jar', '-dev.jar')):
        raise ValueError('Supply the installable JAR, not sources/dev')
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        meta = json.loads(jar.read('fabric.mod.json'))
        if meta.get('id') != 'tflcore' or meta.get('name') != 'TFL Client':
            raise ValueError('Incorrect TFL loader identity')
        if meta.get('environment') != 'client' or meta.get('license') != 'GPL-3.0-only':
            raise ValueError('Incorrect environment/license')
        deps = meta.get('depends', {})
        if deps.get('minecraft') != '26.3' or deps.get('java') != '>=25':
            raise ValueError('Wrong Minecraft or Java target')
        if deps.get('fabricloader') != '>=0.19.5' or deps.get('fabric-api') != '0.161.0+26.3':
            raise ValueError('Runtime dependencies do not match the certified build pins')
        if meta.get('breaks', {}).get('trsclient') != '*':
            raise ValueError('Original TRS conflict must be explicit')
        if meta.get('jars'):
            raise ValueError('Default TFL artifact must not bundle unaudited optimization binaries')
        required = {'LICENSE_trsclient', 'META-INF/TFL-NOTICE.txt', 'META-INF/chat-heads-MPL-2.0.txt',
                    'META-INF/licenses/Inter-OFL.txt', 'assets/trsclient/font/nunito-regular.ttf', 'assets/trsclient/font/nunito-semibold.ttf', 'assets/trsclient/font/nunito-bold.ttf', 'META-INF/licenses/Nunito-OFL.txt',
                    'assets/trsclient/font/ui.json', 'assets/trsclient/textures/menu/black-hole.png',
                    'trsclient.mixins.json', 'dev/theredstonee/trsclient/TrsClient.class'}
        if missing := required - names:
            raise ValueError('Missing runtime resources/notices: ' + ', '.join(sorted(missing)))
        if any(n.startswith('online/tflives/tflcore/') or n.startswith('dev/theredstonee/trsclient/dev/') or n.startswith('org/junit/') or n.endswith('.key') for n in names):
            raise ValueError('Old implementation/test/private-key material in distribution')
        for face in ('ui','ui-semibold','ui-bold'):
            validate_ttf_resources(names, json.loads(jar.read('assets/trsclient/font/' + face + '.json')))
        for entries in meta['entrypoints'].values():
            for entry in entries:
                if entry.replace('.', '/') + '.class' not in names:
                    raise ValueError('Missing entrypoint: ' + entry)
        mixins = json.loads(jar.read('trsclient.mixins.json'))
        for entry in mixins.get('client', []) + mixins.get('mixins', []):
            if (mixins['package'] + '.' + entry).replace('.', '/') + '.class' not in names:
                raise ValueError('Missing mixin: ' + entry)
        for name in names:
            if name.endswith('.class'):
                validate_production_references(jar.read(name))
            if name.endswith('.json') and name != 'fabric.mod.json':
                json.loads(jar.read(name))
    return meta


def test_summary(directory):
    reports = sorted(directory.glob('TEST-*.xml'))
    if not reports:
        raise ValueError('No test reports: execute the common test suite first')
    totals = {k: 0 for k in ('tests', 'failures', 'errors', 'skipped')}
    for path in reports:
        suite = ET.parse(path).getroot()
        for key in totals:
            totals[key] += int(suite.get(key, 0))
    if totals['tests'] <= totals['skipped'] or totals['failures'] or totals['errors']:
        raise ValueError('Test suite is not passing: ' + json.dumps(totals))
    return totals


def build_manifest(jar, out):
    meta = inspect_jar(jar)
    summary = test_summary(ADOPTED / 'common/build/test-results/test')
    revision = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(['git', 'status', '--porcelain'], cwd=ROOT, text=True).strip())
    out.mkdir(parents=True, exist_ok=True)
    target = out / jar.name
    if target.resolve() != jar.resolve():
        shutil.copy2(jar, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    manifest = {
        'schemaVersion': 1, 'kind': 'local-build', 'modId': meta['id'], 'version': meta['version'],
        'channel': 'development', 'minecraft': ['26.3'], 'loader': {'id': 'fabric', 'minimum': meta['depends']['fabricloader'].removeprefix('>=')},
        'java': {'minimum': 25},
        'artifact': {'file': target.name, 'sha256': digest, 'size': target.stat().st_size},
        'runtimeDependencies': meta['depends'], 'testedDependencyPins': {'fabricLoader': '0.19.5', 'fabricApi': '0.161.0+26.3'},
        'conflicts': ['trsclient', 'tflcore-0.3.x'],
        'source': {'repository': 'TFLivesStudio/TFLClientMOD', 'commit': revision, 'workingTreeModified': dirty, 'license': 'GPL-3.0-only'},
        'validation': {'commonTests': summary, 'inGame': 'see docs/TESTING.md; never inferred from compilation', 'stableApproved': False},
        'publication': None,
    }
    (out / 'tflcore-build.json').write_text(json.dumps(manifest, indent=2) + '\n')
    (out / (target.name + '.sha256')).write_text(digest + '  ' + target.name + '\n')
    return manifest


def check_instance(mods):
    """Inspect loader metadata, not filenames. No changes to the instance."""
    found = []
    for file in sorted(mods.glob('*.jar')):
        with zipfile.ZipFile(file) as jar:
            if 'fabric.mod.json' in jar.namelist():
                meta = json.loads(jar.read('fabric.mod.json'))
                if meta.get('id') in ('trsclient', 'tflcore'):
                    found.append({'file': file.name, 'id': meta['id'], 'version': meta.get('version')})
    if len(found) > 1:
        raise ValueError('Incompatible duplicate clients: ' + ', '.join(f['file'] for f in found))
    if found and found[0]['id'] == 'trsclient':
        raise ValueError('Original TRS installed; replace it before installing TFL Core')
    return found


def make_mrpack(jar, source, output):
    meta = inspect_jar(jar)
    test_summary(ADOPTED / 'common/build/test-results/test')
    if not source.is_file() or not zipfile.is_zipfile(source):
        raise ValueError('Complete corresponding-source ZIP is required')
    with zipfile.ZipFile(source) as archive:
        required = {'vendor/trs-client/LICENSE', 'vendor/trs-client/fabric/build.gradle',
                    'vendor/trs-client/common/src/main/java/dev/theredstonee/trsclient/core/product/Product.java'}
        if not required.issubset(archive.namelist()):
            raise ValueError('This is not the complete adopted project source')
    api = json.loads((ROOT / 'distribution/fabric-api.json').read_text())
    index = {
        'formatVersion': 1, 'game': 'minecraft', 'versionId': meta['version'] + '-local',
        'name': 'TFL Client development validation',
        'summary': 'Local development pack; not certified for stable distribution.',
        'files': [{'path': 'mods/' + api['file'], 'hashes': api['hashes'],
                   'env': {'client': 'required', 'server': 'unsupported'},
                   'downloads': [api['url']], 'fileSize': api['size']}],
        'dependencies': {'minecraft': '26.3', 'fabric-loader': '0.19.5'},
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED) as pack:
        pack.writestr('modrinth.index.json', json.dumps(index, indent=2) + '\n')
        pack.write(jar, 'overrides/mods/' + jar.name)
        pack.write(source, 'overrides/tflcore-corresponding-source.zip')
        pack.write(ROOT / 'COPYING', 'overrides/TFL-COPYING.txt')
    return {'file': str(output), 'sha256': hashlib.sha256(output.read_bytes()).hexdigest(), 'kind': 'local-development-mrpack'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    verify = sub.add_parser('verify'); verify.add_argument('jar', type=Path)
    package = sub.add_parser('package'); package.add_argument('jar', type=Path); package.add_argument('--out', type=Path, default=ROOT / 'distribution/build')
    instance = sub.add_parser('check-instance'); instance.add_argument('mods', type=Path)
    mrpack = sub.add_parser('mrpack'); mrpack.add_argument('jar', type=Path); mrpack.add_argument('source', type=Path)
    mrpack.add_argument('--out', type=Path, default=ROOT / 'distribution/build/tflcore-validation.mrpack')
    args = parser.parse_args()
    if args.command == 'verify':
        result = inspect_jar(args.jar)
    elif args.command == 'package':
        result = build_manifest(args.jar, args.out)
    elif args.command == 'mrpack':
        result = make_mrpack(args.jar, args.source, args.out)
    else:
        result = check_instance(args.mods)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
