# Ill Eagle modifications

Ill Eagle is an independent modified version of HeliBoard 4.2-beta1. HeliBoard is itself derived from OpenBoard and the Android Open Source Project LatinIME.

## Modification identity and dates

- Project: Ill Eagle
- Developer identity for Ill Eagle changes: MLCRIP
- Modification period documented by this repository: 2026, through 23 October 2026
- Upstream basis: HeliBoard 4.2-beta1 / OpenBoard / AOSP LatinIME
- Combined-program licence: GPL-3.0-only

Existing copyright and licence notices for upstream and third-party material remain applicable. This document identifies Ill Eagle changes; it does not replace those notices or claim authorship of inherited work.

## Ill Eagle changes

The 2026 Ill Eagle work includes:

- Ill Eagle project naming and independent-project documentation.
- An offline rule-based English transformation engine applied after normal keyboard correction.
- Configurable single-word, suffix, phrase, context, and grammar rule categories.
- Support for multiple replacement alternatives selected at runtime.
- Bounded in-memory word and sentence history with boundary clearing.
- JSON ruleset loading, validation, transactional import, restoration, and per-category controls.
- Settings navigation and controls for the transformation features.
- Tests and development documentation for those features.
- Licence, attribution, navigation, and branding cleanup for future distribution.
- Independent Android application identity `net.mlcrip.illeagle`, including distinct debug and provider identifiers so Ill Eagle can coexist with HeliBoard.

The detailed implementation history remains available in Git. Files inherited from upstream may also contain their own `modified` markers and SPDX identifiers.

## Deliberately deferred work

As of the date above, the following items are not represented as complete:

- clearance or removal of all bundled dictionaries;
- replacement of the inherited HeliBoard launcher artwork;
- final release dependency and native-runtime notice collection;
- creation of a signed release APK/AAB and an immutable matching source tag.

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and [docs/DICTIONARY_LICENSING.md](docs/DICTIONARY_LICENSING.md) for the current compliance status.
