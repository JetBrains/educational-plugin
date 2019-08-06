package com.jetbrains.edu.learning.codeforces.api

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.jetbrains.edu.learning.courseFormat.Course
import java.time.Duration
import java.time.ZonedDateTime

class ContestInfo : Course() {
  override fun getId(): Int {
    return super.myId
  }

  @JsonSetter("id")
  override fun setId(id: Int) {
    super.setId(id)
  }

  @JsonSetter("name")
  override fun setName(name: String?) {
    super.setName(name)
  }

  @JsonProperty("type")
  lateinit var typeString: String

  val type: ContestType
    @JsonIgnore
    get() = ContestType.valueOf(typeString)

  @JsonProperty("phase")
  lateinit var phaseString: String

  val phase: ContestPhase
    @JsonIgnore
    get() = ContestPhase.valueOf(phaseString)

  @JsonProperty("frozen")
  var frozen: Boolean = false

  @JsonProperty("durationSeconds")
  var durationSeconds: Long = -1

  val duration: Duration
    @JsonIgnore
    get() = durationSeconds.toDuration()

  @JsonProperty("startTimeSeconds")
  var startTimeSeconds: Long = -1

  val startTime: ZonedDateTime
    @JsonIgnore
    get() = startTimeSeconds.toZonedDateTime()

  val endTime: ZonedDateTime
    @JsonIgnore
    get() = (startTimeSeconds + durationSeconds).toZonedDateTime()

  @JsonProperty("relativeTimeSeconds")
  var relativeTimeSeconds: Long = -1

  val relativeTime: Duration
    @JsonIgnore
    get() = relativeTimeSeconds.toDuration()

  // TODO some more
}

enum class ContestType {
  CF,
  IOI,
  ICPC
}

enum class ContestPhase {
  BEFORE,
  CODING,
  PENDING_SYSTEM_TEST,
  SYSTEM_TEST,
  FINISHED
}