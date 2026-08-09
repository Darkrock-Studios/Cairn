package com.darkrockstudios.cairn.sound

import androidx.compose.runtime.Composable

/**
 * Minimal PCM playback surface each platform implements. All synthesis is
 * common code; actuals only move samples into the platform's audio API.
 *
 * Sounds are sonification: they must respect silent switches, never request
 * audio focus, and never interrupt the user's media.
 */
internal interface PcmPlayer {
    /** Fire-and-forget playback of a mono float buffer. */
    fun playOneShot(samples: FloatArray, sampleRate: Int)

    /** Start (or restart) the looping buffer at gain 0. */
    fun startLoop(samples: FloatArray, sampleRate: Int)

    /** 0..1 linear gain applied to the loop. */
    fun setLoopGain(gain: Float)

    fun stopLoop()

    /** User gesture hook — wasm resumes its AudioContext here; others no-op. */
    fun unlock()

    fun dispose()
}

@Composable
internal expect fun createPcmPlayer(): PcmPlayer
