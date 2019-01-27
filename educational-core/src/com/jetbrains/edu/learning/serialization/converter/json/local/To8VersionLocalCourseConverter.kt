package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.COURSE_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.PROGRAMMING_LANGUAGE

class To8VersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val language = localCourse.get(PROGRAMMING_LANGUAGE)?.asText() ?: ""
    var courseType = EduNames.PYCHARM
    if ("edu-android" == language) {
      localCourse.put(PROGRAMMING_LANGUAGE, EduNames.KOTLIN)
      courseType = EduNames.ANDROID
    }
    localCourse.put(COURSE_TYPE, courseType)

    return localCourse
  }
}
