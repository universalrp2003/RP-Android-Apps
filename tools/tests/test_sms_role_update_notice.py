import importlib.util
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('role_smoke_ui', ROOT/'smsguard/tools/role_smoke_ui.py')
ui = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ui)


class SmsRoleUpdateNoticeTest(unittest.TestCase):
    def test_only_exact_app_owned_update_notice_can_be_deferred(self):
        def nodes(labels, package=ui.PACKAGE):
            return [ET.Element('node', text=t, package=package, enabled='true', bounds='[10,20][30,40]') for t in labels]
        for labels in ui.NOTICES:
            with self.subTest(labels=labels):
                ns = nodes(labels)
                self.assertIn(ui.known_update_later(ns).get('text'), ('Later', 'பிறகு'))
                self.assertIsNone(ui.known_update_later(nodes(labels, 'com.google.android.gms')))
                self.assertIsNone(ui.known_update_later(nodes(labels, 'com.android.permissioncontroller')))
                self.assertIsNone(ui.known_update_later(nodes(labels | {'Harmful app blocked'})))
                self.assertIsNone(ui.known_update_later(nodes(labels | {'SMS மட்டும் ஏற்கிறேன் — தொடர்க'})))
                button = ui.known_update_later(ns)
                button.set('enabled', 'false')
                self.assertIsNone(ui.known_update_later(ns))
        for labels in (set(), {'Later'}, {'Update available', 'Later'}, {'Harmful app blocked', 'Later'}, {'SMS Guard', 'Set as default'}):
            self.assertIsNone(ui.known_update_later(nodes(labels)))
        source = (ROOT/'smsguard/tools/default-role-smoke.py').read_text()
        for assertion in ("assert installed_hash() == expected_hash", "assert uid() == old_uid", "assert holder()==PKG", "'incoming SMS persisted and visible'", "'SMS survives normal update'", "'Declining changed default app'", "'Cancelling Android chooser changed role'"):
            self.assertIn(assertion, source)
