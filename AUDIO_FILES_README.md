# Audio Files Required for Mindfulness Feature

Place the following audio files in this directory (`res/raw/`).

## Sources (Royalty-Free / No Attribution Required)
- **Pixabay Music**: https://pixabay.com/music/search/meditation/
- **Pixabay Sounds**: https://pixabay.com/sound-effects/
- **Freesound.org**: https://freesound.org (CC0 sounds)

## Required Files

### Ambient Sounds (2-3 minute loops, ~1-2MB each)
| Filename | Description | Free Tier |
|----------|-------------|-----------|
| `ambient_rain.mp3` | Gentle rainfall sound | YES |
| `ambient_forest.mp3` | Birds and rustling leaves | YES |
| `ambient_ocean.mp3` | Ocean waves on shore | YES |
| `ambient_wind.mp3` | Soft wind/breeze | NO |
| `ambient_fire.mp3` | Crackling fireplace | NO |
| `ambient_thunder.mp3` | Distant thunderstorm | NO |
| `ambient_night.mp3` | Crickets, owls, night sounds | NO |
| `ambient_stream.mp3` | Babbling brook/stream | NO |

### Background Music (2-3 minute loops, ~2-3MB each)
| Filename | Description | Free Tier |
|----------|-------------|-----------|
| `music_calm.mp3` | Soft ambient tones | YES |
| `music_piano.mp3` | Gentle piano melody | NO |
| `music_bowls.mp3` | Tibetan singing bowls | NO |
| `music_flute.mp3` | Native/meditation flute | NO |

### Chimes/Effects (3-5 seconds, ~50-100KB each)
| Filename | Description |
|----------|-------------|
| `chime_bell.mp3` | Simple meditation bell |
| `chime_gong.mp3` | Deep gong sound |
| `chime_bowl.mp3` | Singing bowl strike |
| `chime_wind.mp3` | Wind chime |

## Audio Specifications
- **Format**: MP3 (or OGG for smaller size)
- **Bitrate**: 128-192 kbps (good quality, small size)
- **Sample Rate**: 44.1 kHz
- **Channels**: Stereo or Mono
- **Looping**: Ambient sounds should loop seamlessly

## Estimated Total Size
- Ambient sounds: ~10-15 MB
- Background music: ~8-12 MB
- Chimes: ~0.5 MB
- **Total**: ~20-30 MB

## Tips for Finding Sounds
1. Search "meditation" or "relaxation" on Pixabay
2. Look for "seamless loop" versions
3. Download highest quality available
4. Test loops for smooth transitions
5. Normalize volume levels across files

## Placeholder Behavior
If audio files are missing, the app will:
- Skip playing that sound (no crash)
- Show a toast/log message for debugging
- Allow user to continue without audio
