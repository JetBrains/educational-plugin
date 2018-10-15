package com.jetbrains.edu.learning.stepik.courseFormat

import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import com.jetbrains.edu.learning.stepik.StepikNames
import java.util.*

class StepikCourseRemoteInfo : RemoteInfo {
  // publish to stepik
  var isPublic: Boolean = false
  var isIdeaCompatible = true
  var id: Int = 0
  var updateDate = Date(0)
  var sectionIds: MutableList<Int> = mutableListOf() // in CC mode is used to store top-level lessons section id
  var instructors: MutableList<Int> = mutableListOf()
  var additionalMaterialsUpdateDate = Date(0)

  var courseFormat = "" //course format in form: "pycharm<version> <language>"

  // do not publish to stepik
  var loadSolutions = true // disabled for reset courses

  fun updateFormat(language: String) {
    val separator = courseFormat.indexOf(" ")
    val version: String
    version = if (separator != -1) {
      courseFormat.substring(StepikNames.PYCHARM_PREFIX.length, separator)
    }
    else {
      JSON_FORMAT_VERSION.toString()
    }

    courseFormat = String.format("%s%s %s", StepikNames.PYCHARM_PREFIX, version, language)
  }
}
