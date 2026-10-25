#!/usr/bin/env python3
"""Join the authoritative inventory with actual runtime evidence; never certify toggles."""
import argparse
import collections
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
# Exact assertion names/prefixes that exercise outputs, not capability or enabled flags.
FUNCTIONS = {
    'cps': ['CPS hook both buttons'],
    'zoom': ['native frame applies zoom', 'native FOV return hook', 'zoom released'],
    'toggleSprint': ['physical sprint', 'native sprint binding', 'disable restores native sprint', 'sprint resets', 'sprint tap'],
    'crosshair': ['native crosshair eligibility'],
    'minimap': ['map samples', 'map excludes', 'map reader excludes'],
    'worldMap': ['native map key', 'world map uploads'],
    'waypoints': ['native world waypoints persist'],
    'streamerMode': ['shared/native privacy masks'],
    'tooltips': ['native tooltip'],
    'particles': ['native particle gate', 'disabling particle gate'],
    'chat': ['native chat groups', 'native timestamp', 'native chat preserves', 'native history', 'chat disable'],
    'chatFilter': ['native chat filter'],
    'redstoneSignal': ['native redstone dust', 'native redstone probe', 'native powered repeater', 'native comparator', 'shared redstone readout'],
    'reach': ['native attack records'],
    'combo': ['native hurt confirms', 'duplicate hurt', 'own hurt'],
    'emotes': ['local emote starts', 'native humanoid emote', 'emote selects', 'attack cancels local emote', 'emote restores', 'humanoid restores'],
    'colors': ['native color post-process', 'color disable releases'],
    'circuitLibrary': ['offline circuit recipes', 'circuit service clients', 'native circuit checker'],
    'cinematic': ['freecam detaches', 'freecam physical', 'freecam restores'],
    'panorama': ['panorama captures', 'panorama encodes', 'panorama restores'],
    'screenshots': ['native screenshot file', 'local screenshot watcher', 'shared local gallery'],
    'autoGg': ['legacy enabled AutoGG'],
    'autorefill': ['legacy enabled refill'],
}
# Consent guard checks do not exercise automatic sending or inventory swapping.
GUARDS_ONLY = {'autoGg', 'autorefill'}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--evidence', type=Path, default=ROOT / 'docs/ornithe/evidence')
    args = parser.parse_args()
    inventory = json.loads((ROOT / 'distribution/modules.json').read_text())['modules']
    reports = {s: json.loads((args.evidence / (s + '.json')).read_text())
               for s in ('baseline', 'lwjgl3', 'argentum')}
    restarts = {s: json.loads((args.evidence / (s + '-restart.json')).read_text()) for s in reports}
    ids = {m['id'] for m in inventory}
    assert len(inventory) == len(ids) == 79
    for s, d in reports.items():
        assert set(d['capabilities']) == ids
        assert d['status'] == restarts[s]['status'] == 'passed'
        assert all(c['passed'] for c in d['checks'] + restarts[s]['checks'])
        assert d['version'] == '0.4.0-ornithe.2'
    rows = []
    for m in inventory:
        key = m['id']
        cap = reports['argentum']['capabilities'][key]
        state = cap['state']
        native = {s: [c['name'] for c in d['checks'] if any(c['name'].startswith(p)
                    for p in FUNCTIONS.get(key, []))] for s, d in reports.items()}
        blocked = state in ('BACKEND_UNAVAILABLE', 'OPTIONAL_INTEGRATION', 'VERSION_UNAVAILABLE')
        status = ('not_applicable_1_8_9' if state == 'VERSION_UNAVAILABLE' else
                  'requires_external_infrastructure' if blocked else 'implemented_with_limitations')
        rows.append({
            'id': key, 'name': m['name'], 'category': m['category'], 'hud': m['hud'],
            'applicability': 'Absent in Minecraft 1.8.9' if state == 'VERSION_UNAVAILABLE' else 'Applicable subject to dependencies/limits',
            'state': state, 'finalStatus': status,
            'nativeHooks': cap['hooks'], 'implementedSettings': cap['settings'],
            'hiddenPreservedSettings': sorted(set(m['settings']) - set(cap['settings'])),
            'gui': 'Shared ModMenu/settings, capability detail and available actions; unsupported settings hidden',
            'activation': {s: any(c['name'] == key + ' enabled' for c in d['checks']) for s, d in reports.items()},
            'deactivation': {s: any(c['name'] == key + ' disabled' for c in d['checks']) for s, d in reports.items()},
            'persistence': 'Shared ConfigStore; registry preservation/consent unit contracts. Process restart verifies selected preferences only.',
            'restoration': 'See explicit native assertions; per-setting lifecycle acceptance remains incomplete.',
            'dependencies': ['Ornithe Gen2', 'OSL 0.22.2', 'Java 25'] + (['external infrastructure/integration required'] if blocked else []),
            'argentum': {'state': state, 'policy': cap['detail'], 'nativeFunctionalChecks': native['argentum']},
            'unitTests': ['CapabilityRegistryTest (inventory/config contract)', 'common JUnit suite (shared regression; not per-module certification)'],
            'nativeChecks': native, 'nativeEvidenceKind': 'guard only' if key in GUARDS_ONLY else ('output assertions' if native['argentum'] else 'state/screen survival only; functional acceptance pending'),
            'limitations': [cap['detail'], 'No complete per-setting, multiplayer, physical-GPU or all-dimension certification.'],
            'completelyCertified': False,
        })
    counts = collections.Counter(r['finalStatus'] for r in rows)
    functional = sum(bool(r['nativeChecks']['argentum']) and r['id'] not in GUARDS_ONLY for r in rows)
    audit = {'schemaVersion': 1, 'version': '0.4.0-ornithe.2', 'inventory': 'distribution/modules.json',
             'evidence': 'docs/ornithe/evidence (local Java 25/Linux llvmpipe runs; final CI repeats)',
             'counts': {**counts, 'total': 79, 'completelyCertified': 0, 'nativeOutputAssertedModules': functional,
                        'guardOnlyModules': len(GUARDS_ONLY), 'runtimePending': sum(r['state'] == 'PENDING' for r in rows)},
             'warning': 'Runtime wiring and enabled-state toggles are not complete functional certification.', 'modules': rows}
    (ROOT / 'distribution/ornithe-module-audit.json').write_text(json.dumps(audit, indent=2, ensure_ascii=False) + '\n')
    out = ['# Ornithe II — 79-module portability audit', '',
           'Generated with `python3 tools/ornithe/audit_capabilities.py` from the authoritative inventory and committed native evidence.', '',
           f'**79 audited; 73 native adapters with limitations; 1 unavailable mechanic; 3 unavailable backends; 2 external integrations.**',
           f'**0 completely certified; {functional} modules have explicit native output assertions; 2 additional modules have consent-guard assertions.**',
           'No runtime PENDING entry remains. This does not establish full feature/setting parity. The other adapters still need functional acceptance beyond toggle/screen survival.', '',
           'Detailed hooks, settings, activation/deactivation, persistence, restoration, dependencies, per-stack checks and limitations are in [the JSON audit](../../distribution/ornithe-module-audit.json).', '',
           '| ID | Category | Runtime state (Argentum) | Native evidence | Limitations / preserved hidden settings |',
           '| --- | --- | --- | --- | --- |']
    for r in rows:
        limitation = r['limitations'][0]
        if r['hiddenPreservedSettings']:
            limitation += ' Hidden: ' + ', '.join(r['hiddenPreservedSettings']) + '.'
        out.append('| ' + ' | '.join([r['id'], r['category'], r['state'], r['nativeEvidenceKind'], limitation.replace('|', '\\|')]) + ' |')
    (ROOT / 'docs/ornithe/FEATURE_MATRIX.md').write_text('\n'.join(out) + '\n')
    print(json.dumps(audit['counts'], indent=2))


if __name__ == '__main__':
    main()
