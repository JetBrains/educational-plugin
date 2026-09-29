import unittest
import requests
import seaborn as sns
from task import sum

class TestCase(unittest.TestCase):
    def test_add(self):
        self.assertEqual(sum(1, 2), 3, msg="error")

    def test_seaborn(self):
        palette = sns.color_palette("deep", 3)
        self.assertEqual(len(palette), 3, msg="error")
