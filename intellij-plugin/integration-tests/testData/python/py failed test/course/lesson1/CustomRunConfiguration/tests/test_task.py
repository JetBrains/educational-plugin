import unittest

from task import hello


class TestCase(unittest.TestCase):
    def test_hello(self):
        self.assertEqual(hello(), "Hello", msg="Error message")
