import unittest

from task import sum


class TestCase(unittest.TestCase):
    def test_add(self):
        self.assertEqual(sum(1, 2), 3, msg="error")

    @unittest.skip("Ignored test")
    def test_ignored(self):
        self.assertEqual(sum(1, 2), 4, msg="error")
