package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import java.util.*

class StepikLessonRemoteInfo : RemoteInfo {
  var id: Int = 0
  var steps: MutableList<Int>? = null
  var isPublic: Boolean = false
  var updateDate = Date(0)
  var unitId = 0
}
