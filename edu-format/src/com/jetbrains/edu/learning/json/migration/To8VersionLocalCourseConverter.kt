package com.jetbrains.edu.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.json.migration.MigrationNames.ANDROID
import com.jetbrains.edu.learning.json.migration.MigrationNames.KOTLIN
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE

class To8VersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val language = localCourse.get(PROGRAMMING_LANGUAGE)?.asText() ?: ""
    var courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduFormatNames.PYCHARM
    if ("edu-android" == language) {
      localCourse.put(PROGRAMMING_LANGUAGE, KOTLIN)
      courseType = ANDROID
    }
    localCourse.put(COURSE_TYPE, courseType)

    return localCourse
  }
}
