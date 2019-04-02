package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.COURSE_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.ENVIRONMENT

class To11VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduNames.PYCHARM
    if (courseType == EduNames.ANDROID) {
      localCourse.put(ENVIRONMENT, EduNames.ANDROID)
      localCourse.put(COURSE_TYPE, EduNames.PYCHARM)
    }
    return localCourse
  }
}
