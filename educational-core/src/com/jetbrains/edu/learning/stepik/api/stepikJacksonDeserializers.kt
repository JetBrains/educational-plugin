package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikNames

class JacksonLessonDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<Lesson>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Lesson {
    val node: JsonNode = jp.codec.readTree(jp)
    val objectMapper = StepikConnector.getMapper(SimpleModule())
    val lesson = objectMapper.treeToValue(node, Lesson::class.java)
    val name = lesson.name
    if (StepikNames.PYCHARM_ADDITIONAL == name) {
      lesson.name = EduNames.ADDITIONAL_MATERIALS
    }
    return lesson
  }
}
