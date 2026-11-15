package dev.shard9.tonegenerator.audio

import kotlin.math.pow

/** Full-scale amplitude understood by the companion firmware. */
internal const val MAX_AMPLITUDE = 32767

/**
 * Maps a 0-100 volume percentage onto the companion's 0-[MAX_AMPLITUDE] amplitude range.
 *
 * The curve is logarithmic so that equal slider steps produce perceptually equal loudness steps.
 * Defined outside [BleManager] so it can be unit tested without a Bluetooth stack.
 */
internal fun volumePercentToAmplitude(percent: Int): Int {
  val ratio = (10.0.pow(percent / 100.0) - 1.0) / 9.0
  return (ratio * MAX_AMPLITUDE).toInt()
}
