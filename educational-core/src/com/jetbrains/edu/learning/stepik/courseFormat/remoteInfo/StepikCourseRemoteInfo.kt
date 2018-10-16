package com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo

import java.util.*

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
