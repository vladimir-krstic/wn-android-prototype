#!/usr/bin/env python3
"""Verify complete locale/key coverage and Android format-token parity."""
from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / "app/src/main/res"
SOURCE = ROOT / "values/strings.xml"
LOCALES = ("values-ru", "values-tr", "values-zh-rCN", "values-zh-rTW")
FORMAT = re.compile(r"%(?:(\d+)\$)?([a-zA-Z%])")

def text(node):
    return "".join(node.itertext())

def signature(value):
    return Counter(match.group(0) for match in FORMAT.finditer(value))

def indexed(root):
    return {(element.tag, element.get("name")): element for element in root}

source = ET.parse(SOURCE).getroot()
source_index = {
    key: element
    for key, element in indexed(source).items()
    if element.get("translatable") != "false"
}
errors = []
for locale in LOCALES:
    path = ROOT / locale / "strings.xml"
    localized = ET.parse(path).getroot()
    localized_index = indexed(localized)
    if set(localized_index) != set(source_index):
        errors.append(f"{locale}: resource keys differ from values")
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
if errors:
    print("\n".join(errors))
    sys.exit(1)
print(f"Verified {len(source_index)} translatable resources across {len(LOCALES)} complete locales.")
