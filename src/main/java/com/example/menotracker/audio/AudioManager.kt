package com.example.menotracker.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.menotracker.data.models.AmbientSound
import com.example.menotracker.data.models.BackgroundMusic
import com.example.menotracker.data.models.ChimeSound
import com.example.menotracker.data.models.SoundLayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

/**
 * Central Audio Manager for Mindfulness Features
 *
 * Handles:
 * - Background ambient sounds (looped)
 * - Background music (looped)
 * - Chime/bell effects
 * - Soundscape mixing (multiple simultaneous sounds)
 * - Volume control and fading
 *
 * Usage:
 * 1. Call initialize(context) on app startup
 * 2. Use playAmbient/playMusic for single background tracks
 * 3. Use Soundscape methods for multi-layer mixing
 * 4. Always call release() when done
 */
object AudioManager {
    private const val TAG = "AudioManager"

    // Context reference
    private var contextRef: WeakReference<Context>? = null

    // MediaPlayers for background audio
    private var ambientPlayer: MediaPlayer? = null
    private var musicPlayer: MediaPlayer? = null

    // SoundPool for chimes (low-latency short sounds)
    private var soundPool: SoundPool? = null
    private val chimeIds = mutableMapOf<ChimeSound, Int>()

    // Soundscape mixer - multiple MediaPlayers
    private val soundscapePlayers = mutableMapOf<AmbientSound, MediaPlayer>()
    private const val MAX_SOUNDSCAPE_LAYERS = 4

    // State
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentAmbient = MutableStateFlow<AmbientSound?>(null)
    val currentAmbient: StateFlow<AmbientSound?> = _currentAmbient.asStateFlow()

    private val _currentMusic = MutableStateFlow<BackgroundMusic?>(null)
    val currentMusic: StateFlow<BackgroundMusic?> = _currentMusic.asStateFlow()

    private val _soundscapeLayers = MutableStateFlow<List<SoundLayer>>(emptyList())
    val soundscapeLayers: StateFlow<List<SoundLayer>> = _soundscapeLayers.asStateFlow()

    // Volume levels
    private var ambientVolume = 0.6f
    private var musicVolume = 0.4f
    private var chimeVolume = 0.8f

    // Handler for fading
    private val handler = Handler(Looper.getMainLooper())

    // ═══════════════════════════════════════════════════════════════
    // INITIALIZATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize AudioManager with context
     * Call this on app startup (e.g., in Application.onCreate)
     */
    fun initialize(context: Context) {
        contextRef = WeakReference(context.applicationContext)
        initializeSoundPool(context)
        Log.d(TAG, "AudioManager initialized")
    }

    private fun initializeSoundPool(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttributes)
            .build()

        // Preload chime sounds (if they exist)
        ChimeSound.entries.forEach { chime ->
            try {
                val resourceId = chime.getResourceId(context)
                if (resourceId != 0) {
                    val id = soundPool?.load(context, resourceId, 1) ?: 0
                    chimeIds[chime] = id
                    Log.d(TAG, "Loaded chime: ${chime.displayName} -> id=$id")
                } else {
                    Log.w(TAG, "Chime file not found: ${chime.resourceName}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load chime ${chime.displayName}: ${e.message}")
            }
        }
    }

    private fun getContext(): Context? = contextRef?.get()

    // ═══════════════════════════════════════════════════════════════
    // AMBIENT SOUNDS (Single background loop)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Play an ambient sound in a loop
     */
    fun playAmbient(sound: AmbientSound, volume: Float = ambientVolume) {
        val context = getContext() ?: run {
            Log.e(TAG, "Context not available")
            return
        }

        val resourceId = sound.getResourceId(context)
        if (resourceId == 0) {
            Log.w(TAG, "Audio file not found: ${sound.resourceName}")
            return
        }

        // Stop existing ambient
        stopAmbient()

        try {
            ambientPlayer = MediaPlayer.create(context, resourceId).apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            ambientVolume = volume
            _currentAmbient.value = sound
            _isPlaying.value = true
            Log.d(TAG, "Playing ambient: ${sound.displayName} at volume $volume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ambient ${sound.displayName}", e)
        }
    }

    /**
     * Stop ambient sound
     */
    fun stopAmbient() {
        ambientPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        ambientPlayer = null
        _currentAmbient.value = null
        updatePlayingState()
        Log.d(TAG, "Stopped ambient")
    }

    /**
     * Set ambient volume
     */
    fun setAmbientVolume(volume: Float) {
        ambientVolume = volume.coerceIn(0f, 1f)
        ambientPlayer?.setVolume(ambientVolume, ambientVolume)
    }

    // ═══════════════════════════════════════════════════════════════
    // BACKGROUND MUSIC
    // ═══════════════════════════════════════════════════════════════

    /**
     * Play background music in a loop
     */
    fun playMusic(music: BackgroundMusic, volume: Float = musicVolume) {
        val context = getContext() ?: run {
            Log.e(TAG, "Context not available")
            return
        }

        val resourceId = music.getResourceId(context)
        if (resourceId == 0) {
            Log.w(TAG, "Music file not found: ${music.resourceName}")
            return
        }

        // Stop existing music
        stopMusic()

        try {
            musicPlayer = MediaPlayer.create(context, resourceId).apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            musicVolume = volume
            _currentMusic.value = music
            _isPlaying.value = true
            Log.d(TAG, "Playing music: ${music.displayName} at volume $volume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play music ${music.displayName}", e)
        }
    }

    /**
     * Stop background music
     */
    fun stopMusic() {
        musicPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        musicPlayer = null
        _currentMusic.value = null
        updatePlayingState()
        Log.d(TAG, "Stopped music")
    }

    /**
     * Set music volume
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        musicPlayer?.setVolume(musicVolume, musicVolume)
    }

    // ═══════════════════════════════════════════════════════════════
    // CHIMES & EFFECTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Play a chime sound (non-blocking, low latency)
     */
    fun playChime(chime: ChimeSound = ChimeSound.BOWL, volume: Float = chimeVolume) {
        val soundId = chimeIds[chime] ?: run {
            Log.w(TAG, "Chime not loaded: ${chime.displayName}")
            return
        }

        val adjustedVolume = volume.coerceIn(0f, 1f)
        soundPool?.play(soundId, adjustedVolume, adjustedVolume, 1, 0, 1f)
        Log.d(TAG, "Playing chime: ${chime.displayName}")
    }

    /**
     * Play session start chime
     */
    fun playStartChime() {
        playChime(ChimeSound.BOWL, chimeVolume)
    }

    /**
     * Play session end chime (slightly different)
     */
    fun playEndChime() {
        playChime(ChimeSound.GONG, chimeVolume)
    }

    /**
     * Set chime volume
     */
    fun setChimeVolume(volume: Float) {
        chimeVolume = volume.coerceIn(0f, 1f)
    }

    // ═══════════════════════════════════════════════════════════════
    // SOUNDSCAPE MIXER (Multiple layers)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Add a sound layer to the soundscape
     */
    fun addSoundscapeLayer(sound: AmbientSound, volume: Float = 0.5f): Boolean {
        if (soundscapePlayers.size >= MAX_SOUNDSCAPE_LAYERS) {
            Log.w(TAG, "Max soundscape layers reached ($MAX_SOUNDSCAPE_LAYERS)")
            return false
        }

        if (soundscapePlayers.containsKey(sound)) {
            Log.w(TAG, "Sound already in soundscape: ${sound.displayName}")
            return false
        }

        val context = getContext() ?: return false

        val resourceId = sound.getResourceId(context)
        if (resourceId == 0) {
            Log.w(TAG, "Audio file not found: ${sound.resourceName}")
            return false
        }

        try {
            val player = MediaPlayer.create(context, resourceId).apply {
                isLooping = true
                setVolume(volume, volume)
                start()
            }
            soundscapePlayers[sound] = player

            // Update state
            val newLayers = _soundscapeLayers.value + SoundLayer(sound, volume, true)
            _soundscapeLayers.value = newLayers
            _isPlaying.value = true

            Log.d(TAG, "Added soundscape layer: ${sound.displayName}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add soundscape layer ${sound.displayName}", e)
            return false
        }
    }

    /**
     * Remove a sound layer from the soundscape
     */
    fun removeSoundscapeLayer(sound: AmbientSound) {
        soundscapePlayers[sound]?.apply {
            if (isPlaying) stop()
            release()
        }
        soundscapePlayers.remove(sound)

        // Update state
        val newLayers = _soundscapeLayers.value.filter { it.sound != sound }
        _soundscapeLayers.value = newLayers
        updatePlayingState()

        Log.d(TAG, "Removed soundscape layer: ${sound.displayName}")
    }

    /**
     * Set volume for a specific soundscape layer
     */
    fun setSoundscapeLayerVolume(sound: AmbientSound, volume: Float) {
        val adjustedVolume = volume.coerceIn(0f, 1f)
        soundscapePlayers[sound]?.setVolume(adjustedVolume, adjustedVolume)

        // Update state
        val newLayers = _soundscapeLayers.value.map {
            if (it.sound == sound) it.copy(volume = adjustedVolume) else it
        }
        _soundscapeLayers.value = newLayers
    }

    /**
     * Clear all soundscape layers
     */
    fun clearSoundscape() {
        soundscapePlayers.values.forEach { player ->
            player.apply {
                if (isPlaying) stop()
                release()
            }
        }
        soundscapePlayers.clear()
        _soundscapeLayers.value = emptyList()
        updatePlayingState()
        Log.d(TAG, "Cleared soundscape")
    }

    /**
     * Load a soundscape preset
     */
    fun loadSoundscapePreset(layers: List<SoundLayer>) {
        clearSoundscape()
        layers.forEach { layer ->
            addSoundscapeLayer(layer.sound, layer.volume)
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VOLUME & FADING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Fade out all audio over specified duration
     */
    fun fadeOutAll(durationMs: Long = 2000L, onComplete: (() -> Unit)? = null) {
        val steps = 20
        val stepDuration = durationMs / steps
        val volumeStep = 1f / steps

        var currentStep = 0

        val fadeRunnable = object : Runnable {
            override fun run() {
                currentStep++
                val multiplier = 1f - (currentStep * volumeStep)

                // Fade ambient
                ambientPlayer?.setVolume(
                    ambientVolume * multiplier,
                    ambientVolume * multiplier
                )

                // Fade music
                musicPlayer?.setVolume(
                    musicVolume * multiplier,
                    musicVolume * multiplier
                )

                // Fade soundscape layers
                soundscapePlayers.forEach { (sound, player) ->
                    val layerVolume = _soundscapeLayers.value
                        .find { it.sound == sound }?.volume ?: 0.5f
                    player.setVolume(layerVolume * multiplier, layerVolume * multiplier)
                }

                if (currentStep < steps) {
                    handler.postDelayed(this, stepDuration)
                } else {
                    stopAll()
                    onComplete?.invoke()
                }
            }
        }

        handler.post(fadeRunnable)
        Log.d(TAG, "Started fade out over ${durationMs}ms")
    }

    /**
     * Stop all audio immediately
     */
    fun stopAll() {
        stopAmbient()
        stopMusic()
        clearSoundscape()
        _isPlaying.value = false
        Log.d(TAG, "Stopped all audio")
    }

    /**
     * Pause all audio (preserves state for resume)
     */
    fun pauseAll() {
        ambientPlayer?.pause()
        musicPlayer?.pause()
        soundscapePlayers.values.forEach { it.pause() }
        _isPlaying.value = false
        Log.d(TAG, "Paused all audio")
    }

    /**
     * Resume all audio
     */
    fun resumeAll() {
        ambientPlayer?.start()
        musicPlayer?.start()
        soundscapePlayers.values.forEach { it.start() }
        _isPlaying.value = ambientPlayer != null || musicPlayer != null || soundscapePlayers.isNotEmpty()
        Log.d(TAG, "Resumed all audio")
    }

    // ═══════════════════════════════════════════════════════════════
    // UTILITY
    // ═══════════════════════════════════════════════════════════════

    private fun updatePlayingState() {
        _isPlaying.value = ambientPlayer?.isPlaying == true ||
                musicPlayer?.isPlaying == true ||
                soundscapePlayers.values.any { it.isPlaying }
    }

    /**
     * Release all resources
     * Call this when app is closing or feature is not needed
     */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        stopAll()
        soundPool?.release()
        soundPool = null
        chimeIds.clear()
        contextRef = null
        Log.d(TAG, "AudioManager released")
    }

    /**
     * Check if any audio is currently playing
     */
    fun isAnyPlaying(): Boolean = _isPlaying.value

    /**
     * Get current volume levels
     */
    fun getVolumes() = Triple(ambientVolume, musicVolume, chimeVolume)
}
