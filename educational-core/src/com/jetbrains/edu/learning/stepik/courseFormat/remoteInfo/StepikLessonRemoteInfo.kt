package com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo

class StepikLessonRemoteInfo : StepikRemoteInfo() {
  var id: Int = 0
  var steps: MutableList<Int>? = null
  var isPublic: Boolean = false
  var unitId = 0
}
