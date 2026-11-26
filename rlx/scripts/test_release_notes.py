"""Tests for release_notes.py. Run: python3 -m unittest discover -s rlx/scripts"""

import os
import tempfile
import unittest

import release_notes

CHANGELOG = """# Changelog

## [2.4.0](https://example.com) (2026-11-26)

### ✨ New Features

* **YouTube - Navigation bar:** Add a setting ([abc1234](https://example.com))
* **Spoof video streams:** Add a client ([abc1234](https://example.com))
* **Reddit - Copy text:** Make titles selectable ([abc1234](https://example.com))
* List contributors for all time, 6 months, 3 months ([abc1234](https://example.com))
* Redesign of the About dialog ([abc1234](https://example.com))
* **YouTube Music:** Add `Glass menus` patch ([abc1234](https://example.com))
"""


class FeaturesTest(unittest.TestCase):
    def test_only_patch_features(self):
        with tempfile.TemporaryDirectory() as root:
            with open(os.path.join(root, "CHANGELOG.md"), "w", encoding="utf-8") as f:
                f.write(CHANGELOG)
            old, release_notes.ROOT = release_notes.ROOT, root
            try:
                found = release_notes.features("2.4.0", None)
            finally:
                release_notes.ROOT = old
        self.assertEqual(found, [
            "**YouTube - Navigation bar:** Add a setting",
            "**YouTube Music:** Add `Glass menus` patch",
            "**Spoof video streams:** Add a client",
        ])


if __name__ == "__main__":
    unittest.main()
