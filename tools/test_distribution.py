import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('distribution', Path(__file__).with_name('distribution.py'))
d = importlib.util.module_from_spec(spec); spec.loader.exec_module(d)


class DistributionTest(unittest.TestCase):
    def test_duplicate_installations_detected_by_metadata_even_if_renamed(self):
        with tempfile.TemporaryDirectory() as tmp:
            mods = Path(tmp)
            for filename, modid in [('renamed.jar', 'tflcore'), ('another.jar', 'trsclient')]:
                with zipfile.ZipFile(mods / filename, 'w') as z:
                    z.writestr('fabric.mod.json', json.dumps({'id': modid, 'version': '1'}))
            with self.assertRaisesRegex(ValueError, 'duplicate'):
                d.check_instance(mods)

    def test_legacy_tfl_and_adopted_tfl_cannot_be_installed_together(self):
        with tempfile.TemporaryDirectory() as tmp:
            mods = Path(tmp)
            for filename in ['old.jar', 'new.jar']:
                with zipfile.ZipFile(mods / filename, 'w') as z:
                    z.writestr('fabric.mod.json', '{"id":"tflcore"}')
            with self.assertRaisesRegex(ValueError, 'duplicate'):
                d.check_instance(mods)

    def test_sources_are_never_installable(self):
        with self.assertRaisesRegex(ValueError, 'sources/dev'):
            d.inspect_jar(Path('tflcore-sources.jar'))

    def test_ttf_provider_uses_minecraft_font_prefix_exactly_once(self):
        names = {'assets/trsclient/font/inter.ttf'}
        d.validate_ttf_resources(names, {'providers': [{'type': 'ttf', 'file': 'trsclient:inter.ttf'}]})
        with self.assertRaisesRegex(ValueError, 'missing resource'):
            d.validate_ttf_resources(names, {'providers': [{'type': 'ttf', 'file': 'trsclient:font/inter.ttf'}]})

    def test_artifact_rejects_linkage_to_excluded_harness(self):
        with self.assertRaisesRegex(ValueError, 'stripped development'):
            d.validate_production_references(b'constant_pool dev/theredstonee/trsclient/dev/AutoTest')
        d.validate_production_references(b'dev.theredstonee.trsclient.dev.AutoTest')
        d.validate_production_references(b'dev/theredstonee/trsclient/perf/HookStats')

    def test_test_reports_must_exist_and_have_executed_tests(self):
        with tempfile.TemporaryDirectory() as tmp:
            p = Path(tmp)
            with self.assertRaisesRegex(ValueError, 'No test reports'):
                d.test_summary(p)
            (p / 'TEST-example.xml').write_text('<testsuite tests="0" failures="0" errors="0" skipped="0"/>')
            with self.assertRaisesRegex(ValueError, 'not passing'):
                d.test_summary(p)
            (p / 'TEST-example.xml').write_text('<testsuite tests="4" failures="1" errors="0" skipped="0"/>')
            with self.assertRaisesRegex(ValueError, 'not passing'):
                d.test_summary(p)
            (p / 'TEST-example.xml').write_text('<testsuite tests="4" failures="0" errors="0" skipped="1"/>')
            self.assertEqual(4, d.test_summary(p)['tests'])


if __name__ == '__main__':
    unittest.main()
