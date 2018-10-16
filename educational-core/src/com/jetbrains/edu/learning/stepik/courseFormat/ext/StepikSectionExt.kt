@file:JvmName("StepikSectionExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo
import java.util.*

var Section.id: Int
  get() = (remoteInfo as? StepikSectionRemoteInfo)?.id ?: 0
  set(id) {
    stepikRemoteInfo.id = id
  }

var Section.position: Int
  get() = (remoteInfo as? StepikSectionRemoteInfo)?.position ?: 0
  set(position) {
    stepikRemoteInfo.position = position
  }

var Section.courseId: Int
  get() = (remoteInfo as? StepikSectionRemoteInfo)?.courseId ?: 0
  set(courseId) {
    stepikRemoteInfo.courseId = courseId
  }

var Section.updateDate: Date
  get() = (remoteInfo as? StepikSectionRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    stepikRemoteInfo.updateDate = date
  }

val Section.stepikRemoteInfo : StepikSectionRemoteInfo
  get() {
    if (remoteInfo !is StepikSectionRemoteInfo) {
      remoteInfo = StepikSectionRemoteInfo()
    }
    return remoteInfo as StepikSectionRemoteInfo
  }

val Section.units: List<Int> get() = (remoteInfo as? StepikSectionRemoteInfo)?.units ?: listOf()

