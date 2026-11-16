"""Checks rlx/defaults.json. Run: python3 -m unittest discover -s rlx/scripts"""

import json
import pathlib
import unittest

DEFAULTS = pathlib.Path(__file__).resolve().parents[1] / "defaults.json"
APPS = ("youtube", "music")


class DefaultsTest(unittest.TestCase):
    def setUp(self):
        with DEFAULTS.open(encoding="utf-8") as file:
            self.defaults = json.load(file)

    def test_apps(self):
        for app in APPS:
            self.assertIsInstance(self.defaults.get(app), dict, app)

    def test_values_are_plain(self):
        # Values are applied with Setting.saveFromString, so only plain values are possible.
        for app in APPS:
            for key, value in self.defaults[app].items():
                self.assertRegex(key, r"^[a-z0-9_]+$", f"{app}: {key}")
                self.assertIsInstance(value, (bool, int, float, str), f"{app}: {key}")

    def test_replaced(self):
        # Earlier RLX defaults of a setting, which a later default replaces once.
        replaced = self.defaults.get("replaced", {})
        for app, settings in replaced.items():
            self.assertIn(app, APPS)
            for key, values in settings.items():
                self.assertIn(key, self.defaults[app], f"{app}: {key} has no current default")
                self.assertIsInstance(values, list, f"{app}: {key}")
                current = str(self.defaults[app][key]).lower()
                for value in values:
                    self.assertIsInstance(value, (bool, int, float, str), f"{app}: {key}")
                    self.assertNotEqual(str(value).lower(), current, f"{app}: {key}")

    def test_theme_color(self):
        # Menus in #0F0F0F as well; a user who selected another color, such as pure black, keeps it.
        youtube = self.defaults["youtube"]
        self.assertEqual(youtube["theme_color_dark"], "custom")
        self.assertEqual(youtube["theme_color_dark_custom"], "#0F0F0F")

    def test_patches_update_notice_off(self):
        for app in APPS:
            self.assertEqual(self.defaults[app].get("patches_update_notice"), "off", app)


if __name__ == "__main__":
    unittest.main()
