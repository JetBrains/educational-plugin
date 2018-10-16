@file:JvmName("StepikSectionExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo
import java.util.*

var Section.id: Int get() = (remoteInfo as? StepikSectionRemoteInfo)?.id ?: 0
  set(id) {
    if (remoteInfo !is StepikSectionRemoteInfo) {
      remoteInfo = StepikSectionRemoteInfo()
    }
    (remoteInfo as StepikSectionRemoteInfo).id = id
  }

val Section.units: List<Int> get() = (remoteInfo as? StepikSectionRemoteInfo)?.units ?: listOf()

var Section.position: Int get() = (remoteInfo as? StepikSectionRemoteInfo)?.position ?: 0
  set(position) {
    if (remoteInfo !is StepikSectionRemoteInfo) {
      remoteInfo = StepikSectionRemoteInfo()
    }
    (remoteInfo as StepikSectionRemoteInfo).position = position
  }

var Section.courseId: Int get() = (remoteInfo as? StepikSectionRemoteInfo)?.courseId ?: 0
  set(courseId) {
    if (remoteInfo !is StepikSectionRemoteInfo) {
      remoteInfo = StepikSectionRemoteInfo()
    }
    (remoteInfo as StepikSectionRemoteInfo).courseId = courseId
  }

var Section.updateDate: Date get() = (remoteInfo as? StepikSectionRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    if (remoteInfo !is StepikSectionRemoteInfo) {
      remoteInfo = StepikSectionRemoteInfo()
    }
    (remoteInfo as StepikSectionRemoteInfo).updateDate = date
  }

