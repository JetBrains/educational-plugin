package com.jetbrains.edu.learning.courseFormat.tasks.data

import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.AttemptBase
import org.jetbrains.annotations.TestOnly
import java.util.*

class DataTaskAttempt : AttemptBase {
  var endDateTime: Date? = null

  // If endDateTime was calculated before and specified in yaml file we shouldn't use isRunning from AttemptBase base class
  // Because we don't save timeLeft value in yaml, so we can't calculate proper endDateTime at
  // com.jetbrains.edu.learning.courseFormat.attempts.AttemptBase.calculateEndDateTime anymore
  override val isRunning: Boolean
    get() {
      @Suppress("LocalVariableName")
      val _endDateTime = endDateTime ?: return true
      return Date() < _endDateTime
    }

  @Suppress("unused") //used for deserialization
  private constructor()

  private constructor(attempt: AttemptBase) : super(attempt.id, attempt.time, attempt.timeLeft) {
    endDateTime = super.calculateEndDateTime()
  }

  @TestOnly
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is DataTaskAttempt) return false

    if (id != other.id) return false
    if (endDateTime != other.endDateTime) return false

    return true
  }

  @TestOnly
  override fun hashCode(): Int {
    var result = id
    result = 31 * result + (endDateTime?.hashCode() ?: 0)
    return result
  }

  companion object {
    fun Attempt.toDataTaskAttempt(): DataTaskAttempt {
      return DataTaskAttempt(this)
    }
  }
}