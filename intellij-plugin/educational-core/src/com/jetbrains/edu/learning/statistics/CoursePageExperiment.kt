package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.diagnostic.logger
import kotlinx.serialization.Serializable

private const val EXPERIMENT_ID = "experiment_id"
private const val EXPERIMENT_VARIANT = "experiment_variant"

private const val MAX_VALUE_LENGTH = 16

@Serializable
data class CoursePageExperiment(val experimentId: String, val experimentVariant: String) {

  companion object {
    private val LOG = logger<CoursePageExperiment>()

    fun fromParams(params: Map<String, String>): CoursePageExperiment? {
      val experimentId = params[EXPERIMENT_ID] ?: return null
      val experimentVariant = params[EXPERIMENT_VARIANT] ?: return null
      if (experimentVariant.length > MAX_VALUE_LENGTH) {
        LOG.warn("Experiment variant is too long: $experimentVariant. Max supported length is $MAX_VALUE_LENGTH")
        return null
      }
      return CoursePageExperiment(experimentId, experimentVariant)
    }
  }
}
