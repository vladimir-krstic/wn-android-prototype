#!/usr/bin/env python3
"""Verify complete locale/key coverage and Android format-token parity."""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
LOCALES = ("values-ru", "values-tr", "values-zh-rCN", "values-zh-rTW")
FORMAT = re.compile(r"%(?:(\d+)\$)?([a-zA-Z%])")

def text(node):
    return "".join(node.itertext())

def signature(value):
    return Counter(match.group(0) for match in FORMAT.finditer(value))

def indexed(root, label, errors):
    result = {}
    for element in root:
        key = element.tag, element.get("name")
        if key in result:
            errors.append(f"{label}: duplicate resource {key[1]}")
        result[key] = element
    return result


def verify_resources(resource_root=ROOT, locales=LOCALES):
    errors = []
    source = ET.parse(resource_root / "values/strings.xml").getroot()
    source_index = {
        key: element
        for key, element in indexed(source, "values", errors).items()
        if element.get("translatable") != "false"
    }
    for locale in locales:
        localized = ET.parse(resource_root / locale / "strings.xml").getroot()
        localized_index = indexed(localized, locale, errors)
        missing = source_index.keys() - localized_index.keys()
        extra = localized_index.keys() - source_index.keys()
        if missing:
            errors.append(f"{locale}: missing resources: {', '.join(sorted(name for _, name in missing))}")
        if extra:
            errors.append(f"{locale}: unexpected resources: {', '.join(sorted(name for _, name in extra))}")
        for key, source_element in source_index.items():
            local_element = localized_index.get(key)
            if local_element is None:
                continue
            if source_element.tag == "string":
                if signature(text(source_element)) != signature(text(local_element)):
                    errors.append(f"{locale}: format tokens differ for {key[1]}")
            elif source_element.tag == "plurals":
                source_items = {item.get("quantity"): text(item) for item in source_element}
                local_items = {item.get("quantity"): text(item) for item in local_element}
                if "other" not in source_items:
                    errors.append(f"values: plural other quantity missing for {key[1]}")
                if len(local_items) != len(local_element) or len(source_items) != len(source_element):
                    errors.append(f"{locale}: duplicate plural quantity for {key[1]}")
                required = (
                    {"one", "few", "many", "other"}
                    if locale == "values-ru"
                    else {"other"}
                    if locale in {"values-zh-rCN", "values-zh-rTW"}
                    else set(source_items)
                )
                if not required.issubset(local_items):
                    errors.append(f"{locale}: plural quantities missing for {key[1]}")
                for quantity, local_value in local_items.items():
                    source_value = source_items.get(quantity, source_items.get("other", source_items.get("one", "")))
                    if signature(source_value) != signature(local_value):
                        errors.append(f"{locale}: format tokens differ for {key[1]}/{quantity}")
    return len(source_index), errors


def main():
    count, errors = verify_resources()
    if errors:
        print("\n".join(errors))
        return 1
    print(f"Verified {count} translatable resources across {len(LOCALES)} complete locales.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
