"""Guard against adding a developer scenario family without a catalog recipe."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/dev/ipf/whitenoise"

class ScenarioCatalogCoverageTest(unittest.TestCase):
    def test_all_scenario_enums_have_catalog_recipes(self):
        catalog = (SOURCE / "scenarios/ScenarioCatalog.kt").read_text()
        enums = set()
        for folder in ("model", "state"):
            for source in (SOURCE / folder).glob("*.kt"):
                enums.update(re.findall(r"enum class (\w*Scenario)\b", source.read_text()))
        registered = set(re.findall(r"scenario<(\w+)>", catalog))
        self.assertFalse(enums - registered, f"Missing catalog families: {sorted(enums - registered)}")

    def test_non_enum_developer_controls_have_catalog_entries(self):
        catalog = (SOURCE / "scenarios/ScenarioCatalog.kt").read_text()
        # These previously lived in switches and action buttons, not scenario enums.
        for control in ("profile-image", "created-chat", "local-key", "startup", "download-queue",
                        "incoming-lock", "vibration-preview", "notification-updates", "background-connection",
                        "speech-audio", "speech-background", "speech-command", "conversation-examples",
                        "relay-import", "performance", "streaming"):
            self.assertIn(f'"{control}"', catalog)

    def test_feature_ui_cannot_offer_scenario_variant_selectors(self):
        # Runtime consumption inside ScenarioEntry is allowed. Presenting fixture
        # variants in feature screens is not: selection belongs to the catalog.
        for source in (SOURCE / "ui").rglob("*.kt"):
            text = source.read_text()
            self.assertIsNone(re.search(r"\b\w*(?:Scenario|Example)\.entries\b", text), str(source))
            self.assertIsNone(re.search(r"\bfun\s+\w*(?:ScenarioChoiceDialog|DeveloperControls|ExampleControls|ScenarioDialog)\b", text), str(source))

    def test_example_injection_is_wired_only_by_catalog_recipes(self):
        fixture_actions = (
            "addConversationArrival", "addMessageReadingExample", "addAttachmentReadingExamples",
            "addAgentConversationExamples", "addNostrEventExamples", "loadDownloadQueueExample",
            "loadRelayImportExample", "previewStartupFailure", "selectProfileImageFailure",
            "setCreatedChatUnavailable", "inventoryExample", "chooseDownloadNetwork",
            "holdDownloadTransfers", "chooseOutcome",
        )
        pattern = re.compile(r"\b(?:" + "|".join(fixture_actions) + r")\b")
        for folder in ("ui", "navigation"):
            for source in (SOURCE / folder).rglob("*.kt"):
                self.assertIsNone(pattern.search(source.read_text()), str(source))

if __name__ == "__main__":
    unittest.main()
