"""Regression coverage for resource errors that ordinary key-set comparison misses."""

from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from verify_locale_resources import verify_resources


class LocaleResourceVerificationTest(unittest.TestCase):
    def verify(self, source, translated, locale="values-tr"):
        with TemporaryDirectory() as directory:
            root = Path(directory)
            for folder, body in (("values", source), (locale, translated)):
                (root / folder).mkdir()
                (root / folder / "strings.xml").write_text(
                    f"<resources>{body}</resources>", encoding="utf-8"
                )
            return verify_resources(root, (locale,))[1]

    def test_duplicate_keys_fail_even_when_both_catalogs_have_the_same_keys(self):
        single = '<string name="title">Title</string>'
        for source, translated in ((single * 2, single), (single, single * 2)):
            self.assertTrue(any("duplicate resource title" in e for e in self.verify(source, translated)))

    def test_missing_and_unexpected_keys_are_named(self):
        errors = self.verify('<string name="title">Title</string>', '<string name="old">Old</string>')
        self.assertIn("values-tr: missing resources: title", errors)
        self.assertIn("values-tr: unexpected resources: old", errors)

    def test_nontranslatable_resources_are_not_required_in_other_locales(self):
        self.assertEqual([], self.verify('<string name="brand" translatable="false">White Noise</string>', ""))

    def test_reordered_arguments_are_valid_but_dropped_or_changed_arguments_fail(self):
        source = '<string name="summary">%1$s: %2$d / %2$d</string>'
        reordered = '<string name="summary">%2$d / %2$d: %1$s</string>'
        self.assertEqual([], self.verify(source, reordered))
        for invalid in ('%2$d: %1$s', '%2$s / %2$d: %1$s'):
            self.assertTrue(self.verify(source, f'<string name="summary">{invalid}</string>'))

    def test_plural_categories_follow_locale_requirements(self):
        source = '<plurals name="count"><item quantity="one">%d item</item><item quantity="other">%d items</item></plurals>'
        chinese = '<plurals name="count"><item quantity="other">%d</item></plurals>'
        self.assertEqual([], self.verify(source, chinese, "values-zh-rCN"))
        self.assertTrue(self.verify(source, chinese, "values-ru"))

    def test_duplicate_plural_quantities_cannot_mask_a_translation(self):
        source = '<plurals name="count"><item quantity="other">%d items</item></plurals>'
        translated = '<plurals name="count"><item quantity="other">wrong</item><item quantity="other">%d</item></plurals>'
        self.assertTrue(any("duplicate plural quantity" in e for e in self.verify(source, translated)))


if __name__ == "__main__":
    unittest.main()
