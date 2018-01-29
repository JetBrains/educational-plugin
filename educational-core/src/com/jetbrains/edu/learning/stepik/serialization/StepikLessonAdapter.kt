package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikWrappers

import java.lang.reflect.Type

class StepikLessonAdapter : JsonDeserializer<Lesson> {
  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Lesson {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java,
                           StepikStepOptionsAdapter()).create()
    val lesson = gson.fromJson(json, Lesson::class.java)
    val name = lesson.name
    if (StepikNames.PYCHARM_ADDITIONAL == name) {
      lesson.name = EduNames.ADDITIONAL_MATERIALS
    }
    return lesson
  }
}
