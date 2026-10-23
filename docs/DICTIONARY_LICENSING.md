# Bundled dictionary licensing status

## Scope and conclusion

This document records the evidence available for the 18 compiled dictionaries in `app/src/main/assets/dicts/` as of 23 October 2026 at Ill Eagle commit `d32e3f17391f5d4ae0b4d51b47fb9289ecb1a00d`.

**None of these dictionaries is declared cleared for public or commercial redistribution by this document.** The files are compiled databases and do not contain a readable per-file licence or source-wordlist notice. The evidence below establishes a connection to HeliBoard's external dictionary collection, but not the exact upstream revision, complete source, copyright chain, or obligations applicable to each local binary.

No dictionary was deleted, replaced, or disabled during this documentation pass.

## Confirmed local evidence

`app/src/main/assets/known_dict_hashes.txt` states that its hashes are for dictionaries in `https://codeberg.org/Helium314/aosp-dictionaries` and notes that the word-list file is relevant. All 18 local binary hashes appear in that list.

| Local file | Bytes | SHA-256 | Present in local known-hash list |
|---|---:|---|:---:|
| `main_bg.dict` | 3,392,444 | `89dbb28105af326a721c6ae7fb45e6a1e8f59c28f8a23f23731ef4d2ad10b90f` | Yes |
| `main_bn.dict` | 1,527,167 | `dd555eee57aceec08200cc293ffafd29a13bb531156e3a36e5a16ff6895d13a6` | Yes |
| `main_de.dict` | 1,606,286 | `ee5de04b6a93af67d2095a68ac1a8709ecdbf838cc5c98795f0b4ad2d192c2c7` | Yes |
| `main_el.dict` | 2,345,513 | `c0794d9e5f85fbe623b7b3e617f2a9d7f45011648b289a0b263dfdafddac9332` | Yes |
| `main_en-GB.dict` | 1,027,321 | `642ff3e5727e81d3a1ebdeddd9226dd7c17166bcd52f8f6e39f34b44be6faabd` | Yes |
| `main_en-US.dict` | 2,956,881 | `bd950ef4b57655120eee65cee62a5d216a63f721d9a8bb759ce2022437840443` | Yes |
| `main_es.dict` | 1,377,071 | `beb2fda8a3bbaeeec95c301c5cce3e40474e01677a4412fb070eee2040c391d3` | Yes |
| `main_fr.dict` | 1,328,933 | `d7d80eaafe52dcdba057cdd7bed279f966c6e16ba501d46bb343ffa67732d62f` | Yes |
| `main_hu.dict` | 426,867 | `e7b8bdb40871257c24e4741ade0ebfbc940a5e5264a8adb5682ad6c8d2e78249` | Yes |
| `main_it.dict` | 1,143,338 | `e89f2927c84d0d12d6b6509cd532a86ff2721747b0f4536c8c4f6e3995dd2b2e` | Yes |
| `main_nl.dict` | 1,407,361 | `537078e3ba369c87063031f03051d80a0ee121a5459f8192811ca254fc959d19` | Yes |
| `main_pl.dict` | 1,193,157 | `5b2d08c14128bcfb9d62db3cd5df8f0af8f7c2e653d9913f72f9f87d64648864` | Yes |
| `main_pt-BR.dict` | 1,064,251 | `2af42e311220b7440c4b62fbdbddc439626621ec8c5873f4146b045337a58b84` | Yes |
| `main_pt-PT.dict` | 1,418,266 | `c9e5607bd63143930b41ca91919bce23e8abca8172ad24a0fdede029398c95d7` | Yes |
| `main_ro.dict` | 6,366,453 | `20ebf36d7297c06a229f6d3a8d18ab2c94cd02653aa23466110f93ac4caaa5ff` | Yes |
| `main_ru.dict` | 2,239,266 | `f7037e4714242fda95a3b4254d47228b9be537c35a125115b138d70318c30e70` | Yes |
| `main_sv.dict` | 1,385,043 | `fd4ff64e40d0c7cd34914f473090b21c266e0ab9631b04a96e799fbe4879a5e4` | Yes |
| `main_tr.dict` | 1,195,386 | `bdd66c0873e65fb71e20f7c8dee85dd7ab9be59f627fff7521790ff8326c9f18` | Yes |

The normal release configuration is expected to package all 18. The ordinary `debug` variant excludes only `main_ro.dict` for size reasons, so the existing debug APK contains 17.

## Current upstream catalogue evidence

The current upstream README at `https://codeberg.org/Helium314/aosp-dictionaries` was inspected on 23 October 2026. It provides source links and, for some current files, a licence description. This is useful provenance evidence but is **not sufficient to assign those current terms to a local historical binary without an exact hash/revision match**.

| Local name | Current catalogue entry/source indication | Licence text shown in current catalogue row | Local status |
|---|---|---|---|
| `main_bg.dict` | Bulgarian legacy; `wordlists/main_bg.source` | No explicit licence in the row | Unresolved |
| `main_bn.dict` | Bangla; `wordlists/main_bn.source` | Source lists described as CC BY 4.0 | Exact revision/attribution unresolved |
| `main_de.dict` | German; `wordlists/main_de.source` | Source lists described as CC BY 4.0 | Exact revision/attribution unresolved |
| `main_el.dict` | Greek; `wordlists/main_el.source` | GPL-3.0 | Exact revision and corresponding source unresolved |
| `main_en-GB.dict` | English UK legacy; `wordlists/main_en_GB.source` | No explicit licence in the row | Unresolved |
| `main_en-US.dict` | English US legacy; `wordlists/main_en_US.source` | No explicit licence in the row | Unresolved |
| `main_es.dict` | Spanish legacy; `wordlists/main_es.source` | No explicit licence in the row | Unresolved |
| `main_fr.dict` | French legacy; `wordlists/main_fr.source` | No explicit licence in the row | Unresolved |
| `main_hu.dict` | Hungarian legacy; `wordlists/main_hu.source` | No explicit licence in the row | Unresolved |
| `main_it.dict` | Italian legacy; `wordlists/main_it.source` | No explicit licence in the row | Unresolved |
| `main_nl.dict` | Dutch legacy; `wordlists/main_nl.source` | No explicit licence in the row | Unresolved |
| `main_pl.dict` | Polish legacy; `wordlists/main_pl.source` | No explicit licence in the row | Unresolved |
| `main_pt-BR.dict` | Portuguese Brazil; `wordlists/main_pt_BR.source` | No explicit licence in the row | Unresolved |
| `main_pt-PT.dict` | Portuguese Portugal legacy; `wordlists/main_pt_PT.source` | No explicit licence in the row | Unresolved |
| `main_ro.dict` | Romanian legacy; `wordlists/main_ro.source` | No explicit licence in the row | Unresolved |
| `main_ru.dict` | Russian legacy; `wordlists/main_ru.source` | No explicit licence in the row | Unresolved |
| `main_sv.dict` | Swedish legacy; `wordlists/main_sv.source` | No explicit licence in the row | Unresolved |
| `main_tr.dict` | Turkish; `wordlists/main_tr.source` | No explicit licence in the row | Unresolved |

The catalogue also contains experimental variants under different terms and recognises Unicode License v3 for some CLDR-derived dictionaries. A filename or language match must not be used to substitute one variant's licence for another.

## Why the known-hash match is not clearance

The local hash file contains 158 recognised hashes without mapping each hash to a filename, revision, source archive, author, or licence. `tools/release.py` fetches the external README and reduces its information to short dictionary-list markers; it does not create a legal manifest. Therefore the current repository cannot establish from local files alone:

- which upstream commit produced each binary;
- whether its preferred source is the linked `.source` recipe, underlying wordlists, or both;
- the copyright and database-right holders;
- required attribution or notice wording;
- whether commercial redistribution is allowed under every underlying source term;
- whether source wordlists or build instructions must accompany the binary; or
- whether a later upstream file with the same name is identical to the bundled file.

## Required resolution before release

For each dictionary:

1. Find the exact upstream commit whose binary SHA-256 matches the local file.
2. Preserve the upstream `.source` recipe and every identified underlying wordlist/source URL.
3. Record the full licence and copyright/attribution for every source.
4. Determine whether database rights or share-alike/source-delivery conditions apply to the compiled dictionary.
5. Confirm commercial redistribution permission.
6. Include required notices and source material with the corresponding Ill Eagle release.
7. Remove the dictionary from release builds if any necessary right or obligation cannot be established.

Until that work is complete, the dictionaries remain a documented release blocker rather than a confirmed incompatibility.
