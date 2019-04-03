package com.jetbrains.edu.learning.serialization.converter.json.local

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.ENVIRONMENT
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.COURSE_TYPE

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
