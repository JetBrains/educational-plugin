package com.jetbrains.edu.learning.serialization.converter.json

import com.google.gson.JsonObject

interface JsonLocalCourseConverter {
  fun convert(localCourse: JsonObject): JsonObject
}
