package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.jetbrains.edu.learning.stepik.api.TIME_LEFT
import com.jetbrains.edu.learning.submissions.ID
import com.jetbrains.edu.learning.submissions.TIME
import java.util.*

abstract class AttemptBase {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(TIME)
  var time: Date = Date()

  @JsonProperty(TIME_LEFT)
  var timeLeft: Long? = null

  open val isRunning: Boolean
    @JsonIgnore
    get() {
      val endDateTime = calculateEndDateTime() ?: return true
      return Date() < endDateTime
    }

  protected fun calculateEndDateTime(): Date? {
    val timeLeft = timeLeft ?: return null
    return Date(time.toInstant().plusSeconds(timeLeft).toEpochMilli())
  }

  constructor()

  constructor(id: Int, time: Date, timeLeft: Long?) {
    this.id = id
    this.time = time
    this.timeLeft = timeLeft
  }
}