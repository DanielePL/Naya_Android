package com.example.menotracker.data.models

import android.content.Context

/**
 * Sound & Audio Models for Mindfulness Features
 *
 * Used by: Breathing, Meditation, Soundscape
 * FREE: 3 ambient sounds + chimes
 * PREMIUM: Full sound library + soundscape mixer
 *
 * NOTE: Audio files must be added to res/raw/ directory.
 * See res/raw/AUDIO_FILES_README.md for details.
 */

// ============================================================
// AMBIENT SOUNDS (Background loops)
// ============================================================

/**
 * Ambient sounds for background during sessions
 * resourceName maps to res/raw/{name}.mp3
 */
enum class AmbientSound(
    val displayName: String,
    val description: String,
    val resourceName: String,
    val emoji: String,
    val isFree: Boolean,
    val category: SoundCategory
) {
    // FREE sounds
    RAIN(
        displayName = "Rain",
        description = "Gentle rainfall",
        resourceName = "ambient_rain",
        emoji = "\uD83C\uDF27\uFE0F",
        isFree = true,
        category = SoundCategory.NATURE
    ),
    FOREST(
        displayName = "Forest",
        description = "Birds and rustling leaves",
        resourceName = "ambient_forest",
        emoji = "\uD83C\uDF32",
        isFree = true,
        category = SoundCategory.NATURE
    ),
    OCEAN(
        displayName = "Ocean",
        description = "Waves on the shore",
        resourceName = "ambient_ocean",
        emoji = "\uD83C\uDF0A",
        isFree = true,
        category = SoundCategory.NATURE
    ),

    // PREMIUM sounds
    WIND(
        displayName = "Wind",
        description = "Soft breeze",
        resourceName = "ambient_wind",
        emoji = "\uD83D\uDCA8",
        isFree = false,
        category = SoundCategory.NATURE
    ),
    FIRE(
        displayName = "Fireplace",
        description = "Crackling fire",
        resourceName = "ambient_fire",
        emoji = "\uD83D\uDD25",
        isFree = false,
        category = SoundCategory.NATURE
    ),
    THUNDER(
        displayName = "Thunder",
        description = "Distant thunderstorm",
        resourceName = "ambient_thunder",
        emoji = "\u26C8\uFE0F",
        isFree = false,
        category = SoundCategory.NATURE
    ),
    NIGHT(
        displayName = "Night",
        description = "Crickets and owls",
        resourceName = "ambient_night",
        emoji = "\uD83C\uDF19",
        isFree = false,
        category = SoundCategory.NATURE
    ),
    STREAM(
        displayName = "Stream",
        description = "Babbling brook",
        resourceName = "ambient_stream",
        emoji = "\uD83C\uDFDE\uFE0F",
        isFree = false,
        category = SoundCategory.NATURE
    );

    /**
     * Get resource ID dynamically (returns 0 if file doesn't exist)
     */
    fun getResourceId(context: Context): Int {
        return context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }

    companion object {
        fun getFreeSounds() = entries.filter { it.isFree }
        fun getPremiumSounds() = entries.filter { !it.isFree }
        fun getByCategory(category: SoundCategory) = entries.filter { it.category == category }
    }
}

/**
 * Background music tracks for meditation
 */
enum class BackgroundMusic(
    val displayName: String,
    val description: String,
    val resourceName: String,
    val emoji: String,
    val isFree: Boolean,
    val durationSeconds: Int
) {
    // FREE music
    CALM(
        displayName = "Calm",
        description = "Soft ambient tones",
        resourceName = "music_calm",
        emoji = "\uD83C\uDFB6",
        isFree = true,
        durationSeconds = 180
    ),

    // PREMIUM music
    PIANO(
        displayName = "Piano",
        description = "Gentle piano melody",
        resourceName = "music_piano",
        emoji = "\uD83C\uDFB9",
        isFree = false,
        durationSeconds = 180
    ),
    SINGING_BOWLS(
        displayName = "Singing Bowls",
        description = "Tibetan singing bowls",
        resourceName = "music_bowls",
        emoji = "\uD83E\uDD4E",
        isFree = false,
        durationSeconds = 180
    ),
    FLUTE(
        displayName = "Flute",
        description = "Native flute melody",
        resourceName = "music_flute",
        emoji = "\uD83C\uDFBC",
        isFree = false,
        durationSeconds = 180
    );

    /**
     * Get resource ID dynamically (returns 0 if file doesn't exist)
     */
    fun getResourceId(context: Context): Int {
        return context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }

    companion object {
        fun getFreeMusic() = entries.filter { it.isFree }
        fun getPremiumMusic() = entries.filter { !it.isFree }
    }
}

// ============================================================
// CHIMES & EFFECTS
// ============================================================

/**
 * Short sound effects (chimes, bells, gongs)
 */
enum class ChimeSound(
    val displayName: String,
    val resourceName: String,
    val durationMs: Int
) {
    BELL(
        displayName = "Bell",
        resourceName = "chime_bell",
        durationMs = 3000
    ),
    GONG(
        displayName = "Gong",
        resourceName = "chime_gong",
        durationMs = 5000
    ),
    BOWL(
        displayName = "Singing Bowl",
        resourceName = "chime_bowl",
        durationMs = 4000
    ),
    CHIME(
        displayName = "Wind Chime",
        resourceName = "chime_wind",
        durationMs = 2500
    );

    /**
     * Get resource ID dynamically (returns 0 if file doesn't exist)
     */
    fun getResourceId(context: Context): Int {
        return context.resources.getIdentifier(resourceName, "raw", context.packageName)
    }
}

// ============================================================
// CATEGORIES
// ============================================================

enum class SoundCategory(val displayName: String) {
    NATURE("Nature"),
    MUSIC("Music"),
    AMBIANCE("Ambiance")
}

// ============================================================
// SOUNDSCAPE MIXER
// ============================================================

/**
 * Active sound layer in soundscape mixer
 */
data class SoundLayer(
    val sound: AmbientSound,
    val volume: Float = 0.7f,
    val isPlaying: Boolean = true
)

/**
 * Soundscape preset (combination of sounds)
 */
data class SoundscapePreset(
    val id: String,
    val name: String,
    val description: String,
    val layers: List<SoundLayer>,
    val isFree: Boolean
) {
    companion object {
        val PRESETS = listOf(
            SoundscapePreset(
                id = "rainy_forest",
                name = "Rainy Forest",
                description = "Rain with forest birds",
                layers = listOf(
                    SoundLayer(AmbientSound.RAIN, 0.6f),
                    SoundLayer(AmbientSound.FOREST, 0.4f)
                ),
                isFree = true
            ),
            SoundscapePreset(
                id = "beach_day",
                name = "Beach Day",
                description = "Ocean waves with gentle wind",
                layers = listOf(
                    SoundLayer(AmbientSound.OCEAN, 0.7f),
                    SoundLayer(AmbientSound.WIND, 0.3f)
                ),
                isFree = false
            ),
            SoundscapePreset(
                id = "cozy_cabin",
                name = "Cozy Cabin",
                description = "Fireplace with rain outside",
                layers = listOf(
                    SoundLayer(AmbientSound.FIRE, 0.6f),
                    SoundLayer(AmbientSound.RAIN, 0.4f)
                ),
                isFree = false
            ),
            SoundscapePreset(
                id = "thunderstorm",
                name = "Thunderstorm",
                description = "Thunder with heavy rain",
                layers = listOf(
                    SoundLayer(AmbientSound.THUNDER, 0.5f),
                    SoundLayer(AmbientSound.RAIN, 0.7f)
                ),
                isFree = false
            ),
            SoundscapePreset(
                id = "night_stream",
                name = "Night Stream",
                description = "Stream with night sounds",
                layers = listOf(
                    SoundLayer(AmbientSound.STREAM, 0.6f),
                    SoundLayer(AmbientSound.NIGHT, 0.4f)
                ),
                isFree = false
            )
        )
    }
}

// ============================================================
// AUDIO SETTINGS
// ============================================================

/**
 * User's sound preferences for sessions
 */
data class SoundPreferences(
    val selectedAmbientSound: AmbientSound? = null,
    val selectedMusic: BackgroundMusic? = null,
    val ambientVolume: Float = 0.6f,
    val musicVolume: Float = 0.4f,
    val chimeEnabled: Boolean = true,
    val selectedChime: ChimeSound = ChimeSound.BOWL,
    val chimeVolume: Float = 0.8f
)
