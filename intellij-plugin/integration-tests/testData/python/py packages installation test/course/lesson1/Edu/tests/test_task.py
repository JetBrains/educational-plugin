import unittest

import hstest
import requests

from task import sum


class TestCase(unittest.TestCase):
    def test_add(self):
        self.assertEqual(sum(1, 2), 3, msg="error")
