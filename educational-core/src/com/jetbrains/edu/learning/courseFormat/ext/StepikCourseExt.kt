@file:JvmName("StepikCourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo

val Course.isAdaptive: Boolean get() = (remoteInfo as? StepikRemoteInfo)?.isAdaptive ?: false
