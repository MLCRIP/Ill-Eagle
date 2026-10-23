# Ill Eagle Keyboard 🦅

> **Development status:** Ill Eagle is under active development. No official Ill Eagle APK has been released yet.

Ill Eagle is an independent modification of [HeliBoard 4.2-beta1](https://github.com/HeliBorg/HeliBoard), itself derived from OpenBoard and the Android Open Source Project (AOSP) LatinIME. It is maintained by **MLCRIP** and is not affiliated with or endorsed by the HeliBoard, OpenBoard, Google, or AOSP maintainers.

Ill Eagle adds an offline, configurable rules engine that can transform English text after normal keyboard correction. The keyboard does not request Internet permission.

## Current features

- Single-word swaps with optional alternative replacements.
- Automatic preservation of common word endings.
- Multiword phrase swaps.
- Context-aware swaps triggered by earlier or later words.
- Subject-and-verb grammar transformations.
- Transactional import of custom JSON rulesets.
- Master and per-category controls.
- RAM-only transformation history that is cleared at sentence and editor boundaries.

The inherited HeliBoard feature set includes configurable layouts and themes, multilingual typing, dictionaries, clipboard history, one-handed mode, split keyboard, and backup/restore support.

## Downloads

There is currently **no official Ill Eagle APK or Google Play release**. Do not treat HeliBoard downloads as Ill Eagle builds.

When a public build is ready, it will be published through the [Ill Eagle releases page](https://github.com/MLCRIP/Ill-Eagle/releases) together with a link to the exact corresponding source code and applicable third-party notices.

## Documentation and project links

- [Source code](https://github.com/MLCRIP/Ill-Eagle)
- [Report an Ill Eagle issue](https://github.com/MLCRIP/Ill-Eagle/issues)
- [Modification history](MODIFICATIONS.md)
- [Third-party notices](THIRD_PARTY_NOTICES.md)
- [Bundled dictionary licensing status](docs/DICTIONARY_LICENSING.md)
- [Keyboard layout documentation](layouts.md)
- [Contribution information](CONTRIBUTING.md)

Ill Eagle does not currently claim an established contributor community, support forum, wiki, or translation service.

## Building

The project is built with Gradle and the Android toolchain. The checked-in build scripts are the authoritative build configuration. A release build should not be distributed until its package identity, bundled dictionaries, dependency notices, signing, and exact corresponding source have been reviewed.

## Licensing

Ill Eagle is a modified HeliBoard work. The combined program, including the Ill Eagle modifications, is distributed under **GNU General Public License version 3 only (`GPL-3.0-only`)**. See [LICENSE](LICENSE).

The repository is not offered as a choice of GPL, Apache, and Creative Commons licences. Individual incorporated components retain their applicable licences, including Apache-2.0, MIT, CC BY-SA 4.0, and other terms identified in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). See also:

- [Apache License 2.0](LICENSE-Apache-2.0), retained for AOSP-derived material and applicable dependencies.
- [Creative Commons Attribution-ShareAlike 4.0](LICENSE-CC-BY-SA-4.0), applicable to the inherited launcher artwork.
- [Bundled dictionary licensing status](docs/DICTIONARY_LICENSING.md), which records unresolved provenance and redistribution questions.

The repository and its public availability do not by themselves resolve the separate licensing requirements of every bundled dictionary or dependency. No claim is made that the project is ready for compliant binary redistribution while the unresolved items remain.

## Upstream projects and resources

These links lead to upstream projects. Their releases, issue trackers, wikis, discussions, contribution rules, and AI policies do not represent Ill Eagle releases or project policies.

- [HeliBoard source](https://github.com/HeliBorg/HeliBoard)
- [HeliBoard wiki](https://github.com/HeliBorg/HeliBoard/wiki)
- [HeliBoard discussions](https://github.com/HeliBorg/HeliBoard/discussions)
- [HeliBoard dictionary collection](https://codeberg.org/Helium314/aosp-dictionaries)
- [OpenBoard](https://github.com/openboard-team/openboard)
- [AOSP LatinIME](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/)

## Credits

- HeliBoard and its contributors.
- OpenBoard and its contributors.
- The Android Open Source Project LatinIME contributors.
- LineageOS, Simple Keyboard, Indic Keyboard, and FlorisBoard contributors whose work is represented in the inherited source history and notices.
- Launcher icon by [Fabian OvrWrt](https://github.com/FabianOvrWrt), with contributions from [The Eclectic Dyslexic](https://github.com/the-eclectic-dyslexic), licensed under CC BY-SA 4.0.
- Bengali Khipro input method by `rank_coder`, licensed under MIT terms preserved in the source and third-party notices.
- Emoji regular-expression material by [Mathias Bynens](https://mathiasbynens.be/), licensed under MIT terms.

Copyright © 2026 MLCRIP for the Ill Eagle modifications. Existing upstream copyright notices remain applicable to their respective material.
