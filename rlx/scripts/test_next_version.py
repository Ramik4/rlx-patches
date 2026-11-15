"""Tests for next_version.py. Run: python3 -m unittest discover -s rlx/scripts"""

import unittest

from next_version import from_morphe, rlx_only


class NextVersionTest(unittest.TestCase):
    def test_dev_maps_to_patch_number(self):
        self.assertEqual(from_morphe("1.51.0-dev.2", []), "1.51.2")
        self.assertEqual(from_morphe("2.3.0-dev.7", ["v2.3.5"]), "2.3.7")

    def test_skipped_dev_skips_number(self):
        self.assertEqual(from_morphe("2.3.0-dev.9", ["v2.3.7"]), "2.3.9")

    def test_stable_opens_next_minor(self):
        self.assertEqual(from_morphe("1.50.0", ["v1.50.4"]), "1.51.0")
        self.assertEqual(from_morphe("v2.9.0", []), "2.10.0")

    def test_rlx_only_release(self):
        self.assertEqual(rlx_only("2.3.7", ["v2.3.7"]), "2.3.8")

    def test_collision_takes_next_free(self):
        # RLX-only 2.3.8 already exists when Morphe 2.3.0-dev.8 arrives.
        self.assertEqual(from_morphe("2.3.0-dev.8", ["v2.3.7", "v2.3.8"]), "2.3.9")
        # The number never goes back down.
        self.assertEqual(from_morphe("2.3.0-dev.8", ["v2.3.7", "v2.3.10"]), "2.3.11")

    def test_stable_after_dev_line_started(self):
        # Morphe 2.2.0 published after RLX 2.3.x releases already exist.
        self.assertEqual(from_morphe("2.2.0", ["v2.3.3"]), "2.3.4")


if __name__ == "__main__":
    unittest.main()
