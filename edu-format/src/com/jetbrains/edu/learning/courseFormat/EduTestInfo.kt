package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.courseFormat.EduTestInfo.PresentableStatus.*
import com.jetbrains.edu.learning.marketplace.api.NAME
import org.jetbrains.annotations.TestOnly

private const val STATUS = "status"

data class EduTestInfo(
  @JsonProperty(NAME) val name: String = "",
  @JsonProperty(STATUS) val status: Int = -1,
  @JsonIgnore val message: String = "",
  @JsonIgnore val details: String? = null,
  @JsonIgnore private val isFinishedSuccessfully: Boolean? = null,
  @JsonIgnore val checkResultDiff: CheckResultDiff? = null
) {
  private val isSuccess: Boolean = if (isFinishedSuccessfully == true) {
    true
  }
  else {
    when (PresentableStatus.get(status)) {
      COMPLETED, SKIPPED, IGNORED -> true
      else -> false
    }
  }

  @JsonCreator
  constructor(
    @JsonProperty(NAME) name: String,
    @JsonProperty(STATUS) status: Int
  ) : this(
    name = name,
    status = status,
    message = "",
    details = null,
    isFinishedSuccessfully = null,
    checkResultDiff = null
  )

  @TestOnly
  constructor(name: String, presentableStatus: PresentableStatus) : this(name, presentableStatus.value)

  override fun toString(): String = "[${PresentableStatus.getPresentableStatus(status)}] $name"

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

    companion object {
      private const val UNKNOWN: String = "Unknown"

      fun get(status: Int): PresentableStatus? = PresentableStatus.values().find { it.value == status }

      fun getPresentableStatus(status: Int): String = get(status)?.toString() ?: UNKNOWN
    }
  }

  companion object {
    fun List<EduTestInfo>.firstFailed(): EduTestInfo? = firstOrNull { !it.isSuccess }
  }
}