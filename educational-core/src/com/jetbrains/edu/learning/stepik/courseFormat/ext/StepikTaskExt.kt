@file:JvmName("StepikTaskExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikTaskRemoteInfo
import java.util.*

var Task.stepId: Int get() = (remoteInfo as? StepikTaskRemoteInfo)?.id ?: 0
  set(id) {
    if (remoteInfo !is StepikTaskRemoteInfo) {
      remoteInfo = StepikTaskRemoteInfo()
    }
    (remoteInfo as? StepikTaskRemoteInfo)?.id = id
  }

var Task.updateDate: Date
  get() = (remoteInfo as? StepikTaskRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    if (remoteInfo !is StepikTaskRemoteInfo) {
      remoteInfo = StepikTaskRemoteInfo()
    }
    (remoteInfo as? StepikTaskRemoteInfo)?.updateDate = date
  }
