# Third-party notices

This file records third-party material identified in the Ill Eagle source tree. It supplements, and does not replace, file-level notices or the full licence texts in this repository.

Ill Eagle as a combined modified HeliBoard program is distributed under GPL-3.0-only. Individual components retain the licences stated below. A component's appearance here does not mean that every unresolved provenance or binary-release obligation has been cleared.

## HeliBoard and OpenBoard

Ill Eagle is based on HeliBoard 4.2-beta1, which is derived from OpenBoard and AOSP LatinIME.

- HeliBoard: https://github.com/HeliBorg/HeliBoard
- OpenBoard: https://github.com/openboard-team/openboard
- Applicable combined-program licence: GNU GPL version 3 only
- Full text: [LICENSE](LICENSE)

Existing author and contributor notices in the source and Git history must be retained.

## Android Open Source Project LatinIME

Substantial Java, Kotlin, resource, build, and native dictionary-engine material originated in AOSP LatinIME. Representative files retain Android Open Source Project copyright notices and Apache-2.0 identifiers.

- Source: https://android.googlesource.com/platform/packages/inputmethods/LatinIME/
- Licence: Apache License 2.0
- Full text: [LICENSE-Apache-2.0](LICENSE-Apache-2.0)

Files subsequently combined or modified under GPL terms retain their applicable AOSP notices.

## Bengali Khipro input method

The complete notice below is preserved from `app/src/main/assets/bn-khipro.mim`:

> MIT License
> Copyright (c) 2024 rank_coder
>
> Permission is hereby granted, free of charge, to any person obtaining a copy
> of this software and associated documentation files (the "Software"), to deal
> in the Software without restriction, including without limitation the rights
> to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
> copies of the Software, and to permit persons to whom the Software is
> furnished to do so, subject to the following conditions:
>
> The above copyright notice and this permission notice shall be included in all
> copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
> IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
> FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
> AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
> LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
> OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
> SOFTWARE.

## Emoji regular-expression pattern

`app/src/main/java/helium314/keyboard/latin/common/Emoji.kt` identifies its source as `https://github.com/mathiasbynens/emoji-test-regex-pattern` and declares `SPDX-License-Identifier: MIT`. The authoritative upstream `LICENSE` contained the following notice when checked on 23 October 2026:

> Copyright Mathias Bynens <https://mathiasbynens.be/>
>
> Permission is hereby granted, free of charge, to any person obtaining
> a copy of this software and associated documentation files (the
> "Software"), to deal in the Software without restriction, including
> without limitation the rights to use, copy, modify, merge, publish,
> distribute, sublicense, and/or sell copies of the Software, and to
> permit persons to whom the Software is furnished to do so, subject to
> the following conditions:
>
> The above copyright notice and this permission notice shall be
> included in all copies or substantial portions of the Software.
>
> THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
> EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
> MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
> NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
> LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
> OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
> WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

## Launcher artwork

The inherited launcher artwork under `art/launcher_icon/` identifies:

- Creator: Fabian OvrWrt — https://github.com/FabianOvrWrt
- Contributor: The Eclectic Dyslexic — https://github.com/the-eclectic-dyslexic
- Source: HeliBoard — https://github.com/HeliBorg/HeliBoard
- Licence: Creative Commons Attribution-ShareAlike 4.0 International
- Full text: [LICENSE-CC-BY-SA-4.0](LICENSE-CC-BY-SA-4.0)

The SVG metadata carries the attribution and source. The generated launcher resources in the Android project are treated as derivatives of that artwork. Before release, MLCRIP must either document whether the artwork was modified and satisfy CC BY-SA attribution/share-alike requirements, or replace it with independently cleared artwork. No endorsement by the artists or upstream project is implied.

## Unicode emoji data

The emoji-generation tool contains Unicode `emoji-test.txt` data, and generated emoji assets are bundled with the app. Files for newer Unicode versions direct users to the Unicode terms of use. The current authoritative Unicode License v3 notice, checked on 23 October 2026 at `https://www.unicode.org/license.txt`, states:

> UNICODE LICENSE V3
>
> COPYRIGHT AND PERMISSION NOTICE
>
> Copyright © 1991-2026 Unicode, Inc.
>
> NOTICE TO USER: Carefully read the following legal agreement. BY
> DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
> SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
> TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
> DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.
> Permission is hereby granted, free of charge, to any person obtaining a
> copy of data files and any associated documentation (the "Data Files") or
> software and any associated documentation (the "Software") to deal in
> the Data Files or Software without restriction, including without limitation
> the rights to use, copy, modify, merge, publish, distribute, and/or sell
> copies of the Data Files or Software, and to permit persons to whom the
> Data Files or Software are furnished to do so, provided that either (a)
> this copyright and permission notice appear with all copies of the Data
> Files or Software, or (b) this copyright and permission notice appear in
> associated Documentation.
> THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
> KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
> MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
> THIRD PARTY RIGHTS.
> IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
> BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
> OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
> WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
> ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
> FILES OR SOFTWARE.
> Except as contained in this notice, the name of a copyright holder shall
> not be used in advertising or otherwise to promote the sale, use or other
> dealings in these Data Files or Software without prior written
> authorization of the copyright holder.

The exact derivation and notice requirements for every older generated emoji asset have not yet been reconstructed. This notice is retained conservatively; final artefact review remains required.

## Bundled dictionaries

The project contains 18 compiled dictionaries with mixed or unresolved source terms. Their current evidence and open questions are recorded in [docs/DICTIONARY_LICENSING.md](docs/DICTIONARY_LICENSING.md). None is declared cleared merely because its hash appears in HeliBoard's known-hash list.

## Gradle/runtime components

The current build declarations and existing debug dependency model identify these direct runtime inputs:

| Component | Declared version | Licence evidence/status |
|---|---:|---|
| AndroidX Core KTX | 1.17.0 | Published POM identifies Apache-2.0. |
| AndroidX RecyclerView | 1.4.0 | Published POM identifies Apache-2.0. |
| AndroidX Autofill | 1.3.0 | Published POM identifies Apache-2.0. |
| AndroidX ViewPager2 | 1.1.0 | Published POM identifies Apache-2.0. |
| kotlinx.serialization JSON | 1.11.0 | Published POM identifies Apache-2.0. |
| AndroidX Compose BOM/material/UI | BOM 2025.11.01 | AndroidX artefacts identify Apache-2.0. |
| AndroidX Navigation Compose | 2.9.8 | Published POM identifies Apache-2.0. |
| Calvin-LL Reorderable | 3.1.0 | Published POM identifies Apache-2.0; author metadata names Calvin Liang. |
| skydoves ColorPicker Compose | 1.1.3 | Published POM identifies Apache-2.0; developer metadata names Jaewoong Eum. |

The existing debug artefact also resolves transitive AndroidX, Kotlin, kotlinx.coroutines, JetBrains annotations, JSpecify, and Guava `listenablefuture` components. Apache-2.0 text is retained in this repository, but the exact release graph and any component-specific NOTICE material must be regenerated and reviewed for the eventual release build.

`com.android.tools:desugar_jdk_libs:2.1.5` may contribute backported runtime code rather than acting only as a build tool. Its applicable GPLv2/Classpath Exception material must be collected from the exact resolved artefact before release. The NDK build also produces `libjni_latinime.so`; required LLVM/libc++ notices must be taken from the exact NDK used for the release.

## Not bundled by the inspected development build

HeliBoard supports user-supplied proprietary glide-typing libraries. No such library was found in the inspected source tree or existing Ill Eagle debug APK. This does not grant Ill Eagle permission to redistribute one later.
