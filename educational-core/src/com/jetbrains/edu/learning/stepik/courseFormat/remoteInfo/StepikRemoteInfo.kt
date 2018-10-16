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

class StepikCourseRemoteInfo : StepikRemoteInfo() {
  // publish to stepik
  var isPublic: Boolean = false
  var isIdeaCompatible = true
  var sectionIds: MutableList<Int> = mutableListOf() // in CC mode is used to store top-level lessons section id
  var instructors: MutableList<Int> = mutableListOf()
  var additionalMaterialsUpdateDate = Date(0)

  var courseFormat = "" //course format in form: "pycharm<version> <language>"

  // do not publish to stepik
  var loadSolutions = true // disabled for reset courses

}