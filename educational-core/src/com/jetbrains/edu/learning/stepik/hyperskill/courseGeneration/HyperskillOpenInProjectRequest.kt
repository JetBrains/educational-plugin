package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

sealed class HyperskillOpenInProjectRequest(val projectId: Int) {
  override fun toString(): String = "projectId=$projectId"
}
class HyperskillOpenStepRequest(projectId: Int, val stepId: Int) : HyperskillOpenInProjectRequest(projectId) {
  override fun toString(): String = "${super.toString()} stepID=$stepId"
}
class HyperskillOpenStageRequest(projectId: Int, val stageId: Int?) : HyperskillOpenInProjectRequest(projectId) {
  override fun toString(): String = "${super.toString()} stageId=$stageId"
}