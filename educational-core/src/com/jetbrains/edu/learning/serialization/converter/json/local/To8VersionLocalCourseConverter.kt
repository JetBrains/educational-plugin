package com.jetbrains.edu.learning.serialization.converter.json.local

import com.google.gson.JsonObject
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.COURSE_TYPE
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.PROGRAMMING_LANGUAGE

class To8VersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: JsonObject): JsonObject {
    val language = localCourse.getAsJsonPrimitive(PROGRAMMING_LANGUAGE)?.asString ?: ""
    var courseType = EduNames.PYCHARM
    if ("edu-android" == language) {
      localCourse.addProperty(PROGRAMMING_LANGUAGE, EduNames.KOTLIN)
      courseType = EduNames.ANDROID
    }
    localCourse.addProperty(COURSE_TYPE, courseType)

    return localCourse
  }
}
