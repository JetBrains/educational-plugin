package com.jetbrains.edu.learning.serialization.converter.json.local

import com.google.gson.JsonObject

interface JsonLocalCourseConverter {
  fun convert(localCourse: JsonObject): JsonObject
}
