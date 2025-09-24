package com.jetbrains.edu.learning.courseFormat

import org.jetbrains.annotations.TestOnly

data class EduTestInfo(
  val name: String,
  val status: Int,
  val message: String,
  val details: String? = null,
  private val isFinishedSuccessfully: Boolean? = null,
  val checkResultDiff: CheckResultDiff? = null
) {
  private val isSuccess: Boolean = if (isFinishedSuccessfully == true) {
    true
  }
  else {
    PresentableStatus.get(status)?.isSuccess() == true
  }

  @Suppress("unused") // used for serialization
  private constructor() : this("", -1, "")

  constructor(
    name: String,
    status: PresentableStatus,
    message: String,
    details: String? = null,
    isFinishedSuccessfully: Boolean? = null,
    checkResultDiff: CheckResultDiff? = null
  ) : this(name, status.value, message, details, isFinishedSuccessfully, checkResultDiff)

  @TestOnly
  constructor(name: String, presentableStatus: PresentableStatus) : this(name, presentableStatus, message = "")

  override fun toString(): String = "[${PresentableStatus.getPresentableStatus(status)}] $name"

  @Suppress("KDocUnresolvedReference")
  /**
   * Values and titles were taken from [com.intellij.execution.testframework.sm.runner.states.TestStateInfo.Magnitude]
   */
  enum class PresentableStatus(val value: Int, val title: String) {
    SKIPPED(0, "Skipped"),
    COMPLETED(1, "Completed"),
    NOT_RUN(2, "Not run"),
    RUNNING(3, "Running"),
    TERMINATED(4, "Terminated"),
    IGNORED(5, "Ignored"),
    FAILED(6, "Failed"),
    ERROR(8, "Error");

    override fun toString(): String = title

    fun isSuccess(): Boolean = this == COMPLETED || this == SKIPPED || this == IGNORED

    companion object {
      private const val UNKNOWN: String = "Unknown"

      fun get(status: Int): PresentableStatus? = entries.find { it.value == status }

      fun getPresentableStatus(status: Int): String = get(status)?.toString() ?: UNKNOWN
    }
  }

  companion object {
    fun List<EduTestInfo>.firstFailed(): EduTestInfo? = firstOrNull { !it.isSuccess }
  }
}
