<div align="center">

# RLX Patches

Morphe patches + VoT + extra features for YouTube and YouTube Music.

[![Latest release](https://img.shields.io/github/v/release/Ramik4/rlx-patches?label=Release&sort=semver&style=for-the-badge)](https://github.com/Ramik4/rlx-patches/releases/latest)

<a href="https://morphe.software/add-source?github=Ramik4/rlx-patches&name=RLX%20Patches">Add to Morphe Manager</a>

</div>

## About

RLX Patches is one patch bundle for **YouTube** and **YouTube Music**. It contains every
patch of [Morphe Patches](https://github.com/MorpheApp/morphe-patches), plus:

- **Voice over translation (Yandex)**, built in: a player button that plays a Yandex voice-over
  translation of the video (YouTube). Settings are in *RLX Extended*.
- **Save to device**, a downloader for videos, audio, thumbnails and subtitles (off by default).
- An **RLX Extended** section in the settings for everything RLX adds.

RLX is an independent project. It is not affiliated with, or endorsed by, Morphe, Google or
YouTube.

## Install

1. Install [Morphe Manager](https://morphe.software).
2. Add this repository as a patch source with the link above, or add
   `https://github.com/Ramik4/rlx-patches` under **Sources → + → Remote**.
3. Patch YouTube or YouTube Music with the **RLX Patches** source.

## Versions

Every release names the Morphe version it is based on, for example
*Based on Morphe 2.3.0-dev.7*.

| Morphe | RLX |
| --- | --- |
| `X.Y.0-dev.N` | `X.Y.N` |
| `X.Y.0` | `X.(Y+1).0` |
| RLX-only release | last RLX version + 1 |

## Credits

| | |
| --- | --- |
| [Morphe](https://github.com/MorpheApp/morphe-patches) | Morphe Patches, which RLX is based on |
| [ReVanced](https://github.com/ReVanced) | Prior work Morphe Patches are based on |
| [YaVoT](https://github.com/sashade8-ship-it/morphe-patches-yavot) by [sashade8](https://github.com/sashade8-ship-it) | Yandex voice over translation, with work of [Jav1x](https://github.com/Jav1x), [anddea](https://github.com/anddea), [MarcaDian](https://github.com/MarcaDian) and [Ramik4](https://github.com/Ramik4); timing and Morphe VoT coordination from the Dual VoT maintainers |
| [vot.js](https://github.com/FOSWLY/vot.js) by FOSWLY, [Voice Over Translation](https://github.com/ilyhalight/voice-over-translation) by ilyhalight | Reference for the Yandex voice over translation API |

## License

RLX Patches is licensed under the [GNU General Public License v3.0](LICENSE), with the
additional terms of GPLv3 section 7 in [NOTICE](NOTICE). Portions are provided by the Morphe
software project. RLX is a modified version of Morphe Patches; all RLX changes and their dates
can be found in the Git history of this repository.
