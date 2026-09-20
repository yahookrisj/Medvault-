package com.example

import com.example.util.FeatureExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun featureExtractor_cosineSimilarity_identicalVectors_returns100Percent() {
    val vectorA = FloatArray(FeatureExtractor.EMBEDDING_SIZE) { 0.1f }
    // Normalize
    var normSq = 0f
    for (v in vectorA) normSq += v * v
    val norm = Math.sqrt(normSq.toDouble()).toFloat()
    for (i in vectorA.indices) vectorA[i] /= norm

    val similarity = FeatureExtractor.calculateSimilarity(vectorA, vectorA)
    assertEquals(100f, similarity, 0.01f)
  }

  @Test
  fun featureExtractor_serialization_worksCorrectly() {
    val vector = FloatArray(10) { it * 0.1f }
    val serialized = FeatureExtractor.serializeVector(vector)
    val deserialized = FeatureExtractor.deserializeVector(serialized)

    assertNotNull(deserialized)
    assertEquals(vector.size, deserialized!!.size)
    assertEquals(vector[0], deserialized[0], 0.001f)
  }
}
