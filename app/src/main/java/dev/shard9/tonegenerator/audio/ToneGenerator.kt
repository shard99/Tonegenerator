package dev.shard9.tonegenerator.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class ToneGenerator {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var isStopping = false

    private var frequency = 100.0
    var overtones = 0
    var channelSelection = 1 // 0: Left, 1: Both, 2: Right
    private var job: Job? = null
    private var recordJob: Job? = null

    var measuredLevel by mutableDoubleStateOf(0.0)

    fun start(scope: CoroutineScope, context: Context) {
        if (isPlaying && !isStopping) return
        if (isPlaying && isStopping) {
            isStopping = false
            return
        }

        isPlaying = true
        isStopping = false

        startPlayback(scope)
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording(scope)
        }
    }

    private fun startPlayback(scope: CoroutineScope) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        job = scope.launch(Dispatchers.Default) {
            var phase = 0.0

            val numSamples = (bufferSize / 4) and -2
            val buffer = FloatArray(numSamples)

            val fadeInSamples = sampleRate // 1 second
            val fadeOutLimit = (sampleRate * 0.1).toInt() // 20ms

            var samplesProcessed = 0
            var fadeOutSamples = 0

            try {
                while (isPlaying) {
                    val currentFreq = frequency
                    val currentOvertones = overtones
                    val currentChannel = channelSelection

                    val activeHarmonics = (0..currentOvertones).filter { k ->
                        (currentFreq * 2.0.pow(k)) <= 20000.0
                    }
                    val gain = if (activeHarmonics.isEmpty()) 0f else 1.0f / activeHarmonics.size

                    for (i in 0 until buffer.size step 2) {
                        val fadeInVolume = if (samplesProcessed < fadeInSamples) {
                            samplesProcessed.toFloat() / fadeInSamples
                        } else {
                            1.0f
                        }

                        val fadeOutVolume = if (isStopping) {
                            val v = 1.0f - (fadeOutSamples.toFloat() / fadeOutLimit)
                            fadeOutSamples++
                            v.coerceIn(0f, 1f)
                        } else {
                            fadeOutSamples = 0
                            1.0f
                        }

                        if (isStopping && (fadeOutVolume <= 0f)) {
                            isPlaying = false
                        }

                        val combinedVolume = fadeInVolume * fadeOutVolume

                        var sampleValue = 0.0
                        for (k in activeHarmonics) {
                            sampleValue += sin(phase * (2.0.pow(k)))
                        }

                        val finalSample = (sampleValue * gain * combinedVolume).toFloat()

                        when (currentChannel) {
                            0 -> {
                                buffer[i] = finalSample
                                buffer[i + 1] = 0f
                            }

                            2 -> {
                                buffer[i] = 0f
                                buffer[i + 1] = finalSample
                            }

                            else -> {
                                buffer[i] = finalSample
                                buffer[i + 1] = finalSample
                            }
                        }

                        phase += 2.0 * PI * currentFreq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        samplesProcessed++
                    }
                    audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            } finally {
                audioTrack?.stop()
                audioTrack?.flush()
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
                isStopping = false
            }
        }
    }

    private fun startRecording(scope: CoroutineScope) {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioRecord?.startRecording()

            recordJob = scope.launch(Dispatchers.Default) {
                val buffer = FloatArray(bufferSize / 4)
                while (isPlaying) {
                    val read = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
                    if (read > 0) {
                        val magSq = calculateGoertzel(buffer, read, frequency)
                        val mag = sqrt(magSq)
                        // The last * 1.5 here is just to get larger differences since
                        // we do not need the top end (I hope)
                        val normalized = ((mag / (read / 2.0)) * 2.0).coerceIn(0.0, 1.0)
                        measuredLevel = normalized
                    }
                }
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                measuredLevel = 0.0
            }
        } catch (_: SecurityException) {
            measuredLevel = 0.0
        }
    }

    private fun calculateGoertzel(samples: FloatArray, len: Int, targetFreq: Double): Double {
        val omega = 2.0 * PI * targetFreq / sampleRate
        val cosine = cos(omega)
        val coeff = 2.0 * cosine

        var q0: Double
        var q1 = 0.0
        var q2 = 0.0

        for (i in 0 until len) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        return q1 * q1 + q2 * q2 - q1 * q2 * coeff
    }

    fun stop() {
        if (isPlaying) {
            isStopping = true
        }
    }

    fun release() {
        isPlaying = false
        isStopping = false
        job?.cancel()
        recordJob?.cancel()
        audioTrack?.release()
        audioTrack = null
        audioRecord?.release()
        audioRecord = null
    }

    fun setFrequency(freq: Double) {
        frequency = freq
    }
}
