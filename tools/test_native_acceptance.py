import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('acceptance', Path(__file__).parent/'verification/check_native_acceptance.py')
acceptance = importlib.util.module_from_spec(spec)
spec.loader.exec_module(acceptance)


class NativeAcceptanceTest(unittest.TestCase):
    def test_filtered_jobs_and_failed_native_steps_never_certify_a_row(self):
        job = dict(name='native (26.3, iris, ubuntu-latest)', conclusion='success', steps=[])
        self.assertEqual(set(), acceptance.successful_rows([job]))
        job['steps'] = [dict(name='Installed 26.3 client, Diorama', conclusion='skipped')]
        self.assertEqual(set(), acceptance.successful_rows([job]))
        job['steps'][0]['conclusion'] = 'failure'
        self.assertEqual(set(), acceptance.successful_rows([job]))
        job['steps'][0]['conclusion'] = 'success'
        self.assertEqual({'26.3, iris'}, acceptance.successful_rows([job]))
        job['conclusion'] = 'failure'
        self.assertEqual(set(), acceptance.successful_rows([job]))

    def test_all_six_combinations_require_their_version_native_step(self):
        jobs = [dict(name=f'native ({combo}, linux)', conclusion='success',
                     steps=[dict(name='Installed '+combo.split(',')[0]+' client and probe', conclusion='success')])
                for combo in acceptance.COMBOS]
        self.assertEqual(set(acceptance.COMBOS), acceptance.successful_rows(jobs))
        jobs[0]['steps'][0]['name'] = 'Installed 26.3 client'
        self.assertNotIn('1.8.9, baseline', acceptance.successful_rows(jobs))
