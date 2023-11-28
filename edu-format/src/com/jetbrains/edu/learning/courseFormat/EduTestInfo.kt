package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.marketplace.api.NAME
import org.jetbrains.annotations.TestOnly

private const val STATUS = "status"

class EduTestInfo private constructor() {
  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(STATUS)
  var status: Int = -1

  constructor(name: String, status: Int): this() {
    this.name = name
    this.status  = status
  }

  @TestOnly
  constructor(name: String, presentableStatus: PresentableStatus): this() {
    this.name = name
    this.status = presentableStatus.value
  }

  @Suppress("unused")
  @get:JsonIgnore
  val presentableStatus: String
    get() = PresentableStatus.values().find { it.value == status }?.title ?: UNKNOWN

  override fun toString(): String = "[$presentableStatus] $name"

  companion object {
    private const val UNKNOWN: String = "Unknown"

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
      ERROR(8, "Error")
    }
  }
}