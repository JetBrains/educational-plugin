package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequest

/**
 * General interface for all Hyperskill Open In IDE requests
 */
sealed interface HyperskillOpenRequest : OpenInIdeRequest

/**
 * Base interface for Open requests when the user has selected Hyperskill project
 */
sealed interface HyperskillOpenWithProjectRequestBase : HyperskillOpenRequest {
  val projectId: Int
}

/**
 * Base interface for Open Step requests
 */
sealed interface HyperskillOpenStepRequestBase : HyperskillOpenRequest {
  val stepId: Int
  val language: String
  val isLanguageSelectedByUser: Boolean
}

/**
 * Open Step In IDE request when the user does not have selected Hyperskill project
 */
class HyperskillOpenStepRequest(
  override val stepId: Int,
  override val language: String,
  override val isLanguageSelectedByUser: Boolean = false
) : HyperskillOpenStepRequestBase {
  override fun toString(): String = "stepId=$stepId language=$language"
}

/**
 * Open Step In IDE request when the user has selected Hyperskill project
 */
class HyperskillOpenStepWithProjectRequest(
  override val projectId: Int,
  override val stepId: Int,
  override val language: String,
  override val isLanguageSelectedByUser: Boolean = false
) : HyperskillOpenWithProjectRequestBase, HyperskillOpenStepRequestBase {
  override fun toString(): String = "projectId=$projectId stepId=$stepId language=$language"
}

/**
 * Open Stage In IDE request when the user has selected Hyperskill project
 */
class HyperskillOpenProjectStageRequest(override val projectId: Int, val stageId: Int?) : HyperskillOpenWithProjectRequestBase {
  override fun toString(): String = "projectId=$projectId stageId=$stageId"
}