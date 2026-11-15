package dev.shard9.tonegenerator.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks down the percentage to amplitude mapping the companion firmware expects.
 *
 * Values here are the wire contract with the companion project: changing them changes what
 * the hardware actually outputs.
 */
class VolumeCurveTest {
  @Test
  fun `zero percent is silent`() {
    assertEquals(0, volumePercentToAmplitude(0))
  }

  @Test
  fun `hundred percent is full scale`() {
    assertEquals(MAX_AMPLITUDE, volumePercentToAmplitude(100))
  }

  @Test
  fun `fifty percent sits on the logarithmic curve`() {
    assertEquals(7872, volumePercentToAmplitude(50))
  }

  @Test
  fun `quarter percent points are on the expected curve`() {
    assertEquals(2833, volumePercentToAmplitude(25))
    assertEquals(16832, volumePercentToAmplitude(75))
  }

  @Test
  fun `amplitude never decreases as percent increases`() {
    val amplitudes = (0..100).map { volumePercentToAmplitude(it) }
    amplitudes.zipWithNext().forEach { (lower, higher) ->
      assertTrue("amplitude must be monotonic, but $higher followed $lower", higher >= lower)
    }
  }

  @Test
  fun `curve is logarithmic rather than linear`() {
    // Equal slider steps are meant to sound equal, so halfway is far below half amplitude.
    assertTrue(volumePercentToAmplitude(50) < MAX_AMPLITUDE / 2)
  }
}
