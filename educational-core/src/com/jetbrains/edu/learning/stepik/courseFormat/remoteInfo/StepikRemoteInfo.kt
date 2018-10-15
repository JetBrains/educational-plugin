package com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import java.util.*

abstract class StepikRemoteInfo : RemoteInfo  {
  var updateDate = Date(0)
  var id: Int = 0
}

class StepikTaskRemoteInfo : StepikRemoteInfo()

class StepikLessonRemoteInfo : StepikRemoteInfo() {
  var steps: MutableList<Int>? = null
  var isPublic: Boolean = false
  var unitId = 0
}

class StepikSectionRemoteInfo : StepikRemoteInfo() {
  var units: MutableList<Int> = mutableListOf()
  var courseId: Int = 0
  var position: Int = 0
}