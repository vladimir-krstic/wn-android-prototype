#!/usr/bin/env python3
"""Build B26 static font resources from the pinned production Android archive.

Requires host-only fonttools==4.64.0. No network calls or application dependencies.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import fontTools
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

BASELINE = '319454889f1c2494dec4a69b5577d98017f44eee'
LICENSE_BASELINE = 'dbe3d528ea82cfefb82ebc2f8e2ca55af7a1060f'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True, help='Pinned production repository root')
    parser.add_argument('--licenses', type=Path, required=True, help='Directory of family-OFL.txt files from pinned google/fonts')
    args = parser.parse_args()
    assert fontTools.__version__ == '4.64.0', 'Use fonttools==4.64.0 for reproducible assets'
    root = Path(__file__).resolve().parents[1]
    source = args.source / 'app/src/main/res/font'
    output = root / 'app/src/main/res/font'; output.mkdir(parents=True, exist_ok=True)
    licenses = root / 'app/src/main/assets/font-licenses'; licenses.mkdir(parents=True, exist_ok=True)
    manifest = {'production_baseline': BASELINE, 'license_baseline': LICENSE_BASELINE,
        'generator': 'fonttools==4.64.0', 'files': [], 'licenses': []}
    notices = []
    for family in ['manrope', 'outfit', 'urbanist', 'figtree']:
        license_source = args.licenses / f'{family}-OFL.txt'
        license_target = licenses / f'{family}-OFL.txt'
        shutil.copyfile(license_source, license_target)
        manifest['licenses'].append({'family': family, 'sha256': digest(license_target),
            'source': f'https://github.com/google/fonts/blob/{LICENSE_BASELINE}/ofl/{family}/OFL.txt'})
        for weight, suffix in [(400,'regular'), (500,'medium'), (600,'semibold'), (700,'bold')]:
            if family == 'manrope' and weight == 400:
                continue  # Preserve production's medium/semibold/bold family mapping.
            original = source / (f'{family}_{suffix}.ttf' if family == 'manrope' else f'{family}_variable.ttf')
            destination = output / f'{family}_{suffix}.ttf'
            font = TTFont(original, recalcTimestamp=False)
            copyright_notice = font['name'].getDebugName(0)
            if copyright_notice not in notices: notices.append(copyright_notice)
            original_cmap = font.getBestCmap()
            if family == 'manrope':
                shutil.copyfile(original,destination)
            else:
                font = instantiateVariableFont(font, {'wght': weight}, inplace=True, updateFontNames=True)
                font.recalcTimestamp = False
                assert 'fvar' not in font
                assert font['OS/2'].usWeightClass == weight
                assert font.getBestCmap() == original_cmap
                font.save(destination, reorderTables=True)
            verified = TTFont(destination)
            assert verified['OS/2'].usWeightClass == weight
            assert 'fvar' not in verified
            manifest['files'].append({'file': str(destination.relative_to(root)), 'family':family,
                'weight':weight, 'sha256':digest(destination), 'source_file': original.name,
                'source_sha256':digest(original), 'glyph_count':len(verified.getGlyphOrder()),
                'copyright':copyright_notice})
    (licenses / 'FONT-NOTICES.txt').write_text('\n\n'.join(notices)+'\n',encoding='utf-8')
    (root / 'docs/references/font-assets.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    print(f"Wrote {len(manifest['files'])} static font resources and four OFL notices.")

if __name__ == '__main__':
    main()
