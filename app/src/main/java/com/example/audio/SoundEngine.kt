package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine {
    private enum class Wave { SINE, TRIANGLE, SQUARE, SAWTOOTH, SWEET_BELL, WARM_PAD }

    private class ToneVoice(
        val wave: Wave,
        val f1: Double,
        val f2: Double,
        val delaySamples: Int,
        val durSamples: Int,
        val vol: Double,
        val isMusic: Boolean,
        val attackSamples: Int
    ) {
        var age = 0
        var phase = 0.0
        var phase2 = 0.0
    }

    private class NoiseVoice(
        val durSamples: Int,
        val vol: Double,
        val cutoffStart: Double,
        val cutoffEnd: Double
    ) {
        var age = 0
        var lpState = 0.0
    }

    private val sampleRate = 44100
    private val tones = CopyOnWriteArrayList<ToneVoice>()
    private val noises = CopyOnWriteArrayList<NoiseVoice>()

    @Volatile var sfxEnabled: Boolean = true
    @Volatile var musicEnabled: Boolean = true
    @Volatile private var foreground: Boolean = true
    @Volatile private var running: Boolean = false

    private var audioThread: Thread? = null
    private var track: AudioTrack? = null

    // 120 BPM eighth-note step (~0.25s per step), 64 steps = 8-bar cheerful loop
    private val spbSec = 60.0 / 120.0 / 2.0
    private val stepSamples = (spbSec * sampleRate).toInt()

    // Sweet C-major party melody (0.0 = rest)
    // Progression: C | G | Am | F | Dm | G | C | G7
    private val melodyNotes = doubleArrayOf(
        // Bar 1 (C): C5 E5 G5 - E5 G5 A5 G5
        523.25, 659.25, 783.99, 0.0, 659.25, 783.99, 880.00, 783.99,
        // Bar 2 (G): D5 F#5->G5 B5 - G5 D5 E5 D5
        587.33, 783.99, 987.77, 0.0, 783.99, 587.33, 659.25, 587.33,
        // Bar 3 (Am): E5 A5 C6 - B5 A5 G5 E5
        659.25, 880.00, 1046.50, 0.0, 987.77, 880.00, 783.99, 659.25,
        // Bar 4 (F): F5 A5 C6 - A5 G5 F5 E5
        698.46, 880.00, 1046.50, 0.0, 880.00, 783.99, 698.46, 659.25,
        // Bar 5 (Dm): D5 F5 A5 - G5 F5 E5 D5
        587.33, 698.46, 880.00, 0.0, 783.99, 698.46, 659.25, 587.33,
        // Bar 6 (G): G5 B5 D6 - B5 A5 G5 F5
        783.99, 987.77, 1174.66, 0.0, 987.77, 880.00, 783.99, 698.46,
        // Bar 7 (C): E5 G5 C6 - G5 E5 G5 A5
        659.25, 783.99, 1046.50, 0.0, 783.99, 659.25, 783.99, 880.00,
        // Bar 8 (G7 -> C): G5 F5 E5 D5 C5 - G4 -
        783.99, 698.46, 659.25, 587.33, 523.25, 0.0, 392.00, 0.0
    )

    // Warm bouncy marimba bass
    private val bassNotes = doubleArrayOf(
        130.81, 0.0, 196.00, 130.81, 0.0, 196.00, 0.0, 164.81, // C
        196.00, 0.0, 146.83, 196.00, 0.0, 146.83, 0.0, 196.00, // G
        220.00, 0.0, 164.81, 220.00, 0.0, 164.81, 0.0, 196.00, // Am
        174.61, 0.0, 130.81, 174.61, 0.0, 130.81, 0.0, 164.81, // F
        146.83, 0.0, 220.00, 146.83, 0.0, 220.00, 0.0, 174.61, // Dm
        196.00, 0.0, 146.83, 196.00, 0.0, 146.83, 0.0, 174.61, // G
        130.81, 0.0, 196.00, 130.81, 0.0, 164.81, 0.0, 196.00, // C
        196.00, 0.0, 146.83, 196.00, 0.0, 130.81, 0.0, 0.0     // G7
    )

    // Gentle sparkle arpeggio chimes
    private val sparkleNotes = doubleArrayOf(
        0.0, 1046.50, 0.0, 1318.51, 0.0, 1567.98, 0.0, 1318.51,
        0.0, 1174.66, 0.0, 1567.98, 0.0, 1975.53, 0.0, 1567.98,
        0.0, 1318.51, 0.0, 1760.00, 0.0, 2093.00, 0.0, 1760.00,
        0.0, 1396.91, 0.0, 1760.00, 0.0, 2093.00, 0.0, 1760.00,
        0.0, 1174.66, 0.0, 1396.91, 0.0, 1760.00, 0.0, 1396.91,
        0.0, 1174.66, 0.0, 1567.98, 0.0, 1975.53, 0.0, 1567.98,
        0.0, 1046.50, 0.0, 1318.51, 0.0, 1567.98, 0.0, 1318.51,
        0.0, 1174.66, 0.0, 1396.91, 0.0, 1567.98, 0.0, 1046.50
    )

    // Warm chord pad triads triggered at the start of each 8-step bar
    private val barChords = arrayOf(
        doubleArrayOf(261.63, 329.63, 392.00), // C
        doubleArrayOf(246.94, 293.66, 392.00), // G
        doubleArrayOf(220.00, 261.63, 329.63), // Am
        doubleArrayOf(220.00, 261.63, 349.23), // F
        doubleArrayOf(220.00, 293.66, 349.23), // Dm
        doubleArrayOf(246.94, 293.66, 392.00), // G
        doubleArrayOf(261.63, 329.63, 392.00), // C
        doubleArrayOf(246.94, 349.23, 392.00)  // G7
    )

    private var musicSampleCounter = 0
    private var musicStep = 0
    private var musicLpState = 0.0

    fun setForeground(isForeground: Boolean) {
        foreground = isForeground
        if (isForeground) {
            ensureStarted()
        } else {
            stopEngine()
        }
    }

    fun release() {
        stopEngine()
    }

    private fun ensureStarted() {
        if (running) return
        running = true
        audioThread = Thread({
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)
            val chunkSize = 512
            val audioTrack = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Throwable) {
                running = false
                return@Thread
            }
            track = audioTrack
            try {
                audioTrack.play()
            } catch (_: Throwable) {
                running = false
                audioTrack.release()
                track = null
                return@Thread
            }

            val pcm = ShortArray(chunkSize)
            val bellAttack = (0.014 * sampleRate).toInt().coerceAtLeast(1)
            val padAttack = (0.18 * sampleRate).toInt().coerceAtLeast(1)
            val bassAttack = (0.015 * sampleRate).toInt().coerceAtLeast(1)
            val musicLpAlpha = 0.24 // gentle warmth low-pass on music bus

            while (running) {
                for (i in 0 until chunkSize) {
                    if (musicEnabled && foreground) {
                        if (musicSampleCounter <= 0) {
                            val idx = musicStep % 64
                            if (idx % 8 == 0) {
                                val chord = barChords[(idx / 8) % barChords.size]
                                for (cf in chord) {
                                    tones.add(
                                        ToneVoice(
                                            wave = Wave.WARM_PAD,
                                            f1 = cf,
                                            f2 = 0.0,
                                            delaySamples = 0,
                                            durSamples = (spbSec * 7.8 * sampleRate).toInt(),
                                            vol = 0.032,
                                            isMusic = true,
                                            attackSamples = padAttack
                                        )
                                    )
                                }
                            }

                            val mf = melodyNotes[idx]
                            if (mf > 0.0) {
                                tones.add(
                                    ToneVoice(
                                        wave = Wave.SWEET_BELL,
                                        f1 = mf,
                                        f2 = 0.0,
                                        delaySamples = 0,
                                        durSamples = (spbSec * 2.2 * sampleRate).toInt(),
                                        vol = 0.085,
                                        isMusic = true,
                                        attackSamples = bellAttack
                                    )
                                )
                            }

                            val bf = bassNotes[idx]
                            if (bf > 0.0) {
                                tones.add(
                                    ToneVoice(
                                        wave = Wave.TRIANGLE,
                                        f1 = bf,
                                        f2 = 0.0,
                                        delaySamples = 0,
                                        durSamples = (spbSec * 1.45 * sampleRate).toInt(),
                                        vol = 0.080,
                                        isMusic = true,
                                        attackSamples = bassAttack
                                    )
                                )
                            }

                            val sf = sparkleNotes[idx]
                            if (sf > 0.0) {
                                tones.add(
                                    ToneVoice(
                                        wave = Wave.SINE,
                                        f1 = sf,
                                        f2 = 0.0,
                                        delaySamples = 0,
                                        durSamples = (spbSec * 1.3 * sampleRate).toInt(),
                                        vol = 0.022,
                                        isMusic = true,
                                        attackSamples = bellAttack
                                    )
                                )
                            }

                            musicStep = (musicStep + 1) % 64
                            musicSampleCounter = stepSamples
                        }
                        musicSampleCounter--
                    }

                    var sfxMix = 0.0
                    var musicMix = 0.0

                    val tIt = tones.iterator()
                    while (tIt.hasNext()) {
                        val v = tIt.next()
                        if (v.isMusic && (!musicEnabled || !foreground)) {
                            tones.remove(v)
                            continue
                        }
                        if (v.age < v.delaySamples) {
                            v.age++
                            continue
                        }
                        val tIdx = v.age - v.delaySamples
                        if (tIdx >= v.durSamples) {
                            tones.remove(v)
                            continue
                        }
                        val frac = tIdx.toDouble() / v.durSamples.toDouble()
                        val freq = if (v.f2 > 0.0) {
                            v.f1 * (v.f2 / v.f1).pow(frac)
                        } else {
                            v.f1
                        }
                        v.phase += freq / sampleRate
                        if (v.phase >= 1.0) v.phase -= 1.0
                        v.phase2 += (freq * 2.0) / sampleRate
                        if (v.phase2 >= 1.0) v.phase2 -= 1.0

                        val env = if (tIdx < v.attackSamples) {
                            tIdx.toDouble() / v.attackSamples.toDouble()
                        } else {
                            val decFrac = (tIdx - v.attackSamples).toDouble() /
                                (v.durSamples - v.attackSamples).coerceAtLeast(1).toDouble()
                            val decayRate = when (v.wave) {
                                Wave.WARM_PAD -> 2.8
                                Wave.SWEET_BELL -> 4.6
                                else -> 6.5
                            }
                            exp(-decayRate * decFrac)
                        }
                        val sample = when (v.wave) {
                            Wave.SINE -> sin(2.0 * PI * v.phase)
                            Wave.TRIANGLE -> 1.0 - 4.0 * abs(v.phase - 0.5)
                            Wave.SQUARE -> if (v.phase < 0.5) 1.0 else -1.0
                            Wave.SAWTOOTH -> 2.0 * v.phase - 1.0
                            Wave.SWEET_BELL -> {
                                // Fundamental sine + gentle 2nd harmonic shimmer
                                0.80 * sin(2.0 * PI * v.phase) + 0.20 * sin(2.0 * PI * v.phase2) * (1.0 - frac)
                            }
                            Wave.WARM_PAD -> {
                                // Smooth rounded triangle-sine blend
                                0.70 * sin(2.0 * PI * v.phase) + 0.30 * (1.0 - 4.0 * abs(v.phase - 0.5))
                            }
                        }
                        val contribution = sample * env * v.vol
                        if (v.isMusic) musicMix += contribution else sfxMix += contribution
                        v.age++
                    }

                    val nIt = noises.iterator()
                    while (nIt.hasNext()) {
                        val n = nIt.next()
                        if (n.age >= n.durSamples) {
                            noises.remove(n)
                            continue
                        }
                        val frac = n.age.toDouble() / n.durSamples.toDouble()
                        val raw = (Random.nextDouble() * 2.0 - 1.0) * (1.0 - frac)
                        val cutoff = n.cutoffStart * (n.cutoffEnd / n.cutoffStart).pow(frac)
                        val rc = 1.0 / (2.0 * PI * cutoff)
                        val dt = 1.0 / sampleRate
                        val alpha = dt / (rc + dt)
                        n.lpState += alpha * (raw - n.lpState)
                        val env = exp(-4.5 * frac)
                        sfxMix += n.lpState * env * n.vol
                        n.age++
                    }

                    musicLpState += musicLpAlpha * (musicMix - musicLpState)
                    val total = sfxMix + musicLpState
                    val scaled = (total * 0.85 * 32767.0).coerceIn(-32767.0, 32767.0)
                    pcm[i] = scaled.toInt().toShort()
                }
                try {
                    audioTrack.write(pcm, 0, chunkSize)
                } catch (_: Throwable) {
                    break
                }
            }
            try {
                audioTrack.stop()
            } catch (_: Throwable) {
            }
            audioTrack.release()
            track = null
        }, "BummmAudioThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun stopEngine() {
        running = false
        tones.clear()
        noises.clear()
        audioThread?.join(150)
        audioThread = null
    }

    private fun addTone(
        wave: Wave,
        f1: Double,
        delaySec: Double,
        durSec: Double,
        vol: Double,
        f2: Double = 0.0
    ) {
        if (!sfxEnabled || !foreground) return
        ensureStarted()
        tones.add(
            ToneVoice(
                wave = wave,
                f1 = f1,
                f2 = f2,
                delaySamples = (delaySec * sampleRate).toInt().coerceAtLeast(0),
                durSamples = (durSec * sampleRate).toInt().coerceAtLeast(1),
                vol = vol,
                isMusic = false,
                attackSamples = (0.012 * sampleRate).toInt().coerceAtLeast(1)
            )
        )
    }

    private fun addNoise(durSec: Double, vol: Double, f1: Double, f2: Double) {
        if (!sfxEnabled || !foreground) return
        ensureStarted()
        noises.add(
            NoiseVoice(
                durSamples = (durSec * sampleRate).toInt().coerceAtLeast(1),
                vol = vol,
                cutoffStart = f1,
                cutoffEnd = f2
            )
        )
    }

    fun click() = addTone(Wave.TRIANGLE, 520.0, 0.0, 0.08, 0.16, 820.0)
    fun pop() = addTone(Wave.SINE, 700.0, 0.0, 0.07, 0.16, 1000.0)

    fun ok(combo: Int) {
        val b = 523.0 * 1.0595.pow(min(combo, 14))
        addTone(Wave.SINE, b, 0.0, 0.10, 0.22)
        addTone(Wave.SINE, b * 1.5, 0.07, 0.16, 0.20)
    }

    fun wrong() {
        addTone(Wave.SAWTOOTH, 240.0, 0.0, 0.30, 0.20, 80.0)
        addTone(Wave.SQUARE, 150.0, 0.05, 0.25, 0.10, 60.0)
    }

    fun boom() {
        addNoise(0.90, 0.55, 2400.0, 60.0)
        addTone(Wave.SINE, 130.0, 0.0, 0.70, 0.50, 34.0)
        addTone(Wave.TRIANGLE, 900.0, 0.0, 0.12, 0.15, 300.0)
    }

    fun tick() = addTone(Wave.SQUARE, 440.0, 0.0, 0.12, 0.12)

    fun go() {
        addTone(Wave.SQUARE, 880.0, 0.0, 0.25, 0.14)
        addTone(Wave.TRIANGLE, 1320.0, 0.05, 0.30, 0.12)
    }

    fun level() {
        val notes = doubleArrayOf(523.0, 659.0, 784.0, 1047.0)
        notes.forEachIndexed { i, f -> addTone(Wave.TRIANGLE, f, i * 0.09, 0.24, 0.22) }
        addTone(Wave.SINE, 1319.0, 0.40, 0.50, 0.20)
    }

    fun out() {
        val notes = doubleArrayOf(392.0, 330.0, 262.0, 196.0)
        notes.forEachIndexed { i, f -> addTone(Wave.TRIANGLE, f, i * 0.14, 0.30, 0.20) }
    }

    fun win() {
        val notes = doubleArrayOf(523.0, 659.0, 784.0, 1047.0, 784.0, 1047.0, 1319.0)
        notes.forEachIndexed { i, f ->
            addTone(Wave.TRIANGLE, f, i * 0.10, 0.25, 0.20)
            addTone(Wave.SQUARE, f * 2.0, i * 0.10, 0.15, 0.05)
        }
    }
}
