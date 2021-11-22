package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

sealed class HyperskillOpenRequest(val projectId: Int) : OpenInIdeRequest {
  override fun toString(): String = "projectId=$projectId"
}
class HyperskillOpenStepRequest(
  projectId: Int,
  val stepId: Int,
  val language: String,
  val isLanguageSelectedByUser: Boolean = false
) : HyperskillOpenRequest(projectId) {
  override fun toString(): String = "${super.toString()} stepID=$stepId language=$language"
}
class HyperskillOpenStageRequest(projectId: Int, val stageId: Int?) : HyperskillOpenRequest(projectId) {
  override fun toString(): String = "${super.toString()} stageId=$stageId"
}