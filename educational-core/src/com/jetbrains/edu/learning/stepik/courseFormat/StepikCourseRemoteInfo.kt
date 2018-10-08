package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import java.util.*

class StepikCourseRemoteInfo : RemoteInfo {
  // publish to stepik
  var isPublic: Boolean = false
  var isAdaptive = false
  var isIdeaCompatible = true
  var id: Int = 0
  var updateDate = Date(0)
  var sectionIds: MutableList<Int> = mutableListOf() // in CC mode is used to store top-level lessons section id
  var instructors: MutableList<Int> = mutableListOf()
  var additionalMaterialsUpdateDate = Date(0)

  // do not publish to stepik
  var loadSolutions = true // disabled for reset courses

}
