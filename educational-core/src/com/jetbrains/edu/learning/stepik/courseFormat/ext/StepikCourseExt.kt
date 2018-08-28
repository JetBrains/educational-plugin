@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourseRemoteInfo
import java.util.*

val StepikCourse.isAdaptive: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isAdaptive ?: false
val StepikCourse.isCompatible: Boolean get() = (remoteInfo as? StepikCourseRemoteInfo)?.isIdeaCompatible ?: false
val StepikCourse.id: Int get() = (remoteInfo as? StepikCourseRemoteInfo)?.id ?: 0

var StepikCourse.updateDate: Date get() = (remoteInfo as? StepikCourseRemoteInfo)?.updateDate ?: Date(0)
  set(date) {
    (remoteInfo as? StepikCourseRemoteInfo)?.updateDate = date
  }

