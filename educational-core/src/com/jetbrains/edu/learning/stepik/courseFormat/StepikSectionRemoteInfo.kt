package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import java.util.*

class StepikSectionRemoteInfo : RemoteInfo {
  var id: Int = 0
  var units: MutableList<Int> = mutableListOf()
  var courseId: Int = 0
  var position: Int = 0
  var updateDate = Date(0)
}
