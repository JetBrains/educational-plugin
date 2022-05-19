package com.jetbrains.edu.learning.checkio.courseFormat

import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.yaml.YamlDeserializerBase

class CheckiOYamlDeserializer : YamlDeserializerBase() {

  override fun deserializeLesson(mapper: ObjectMapper, configFileText: String): Lesson {
    return mapper.readValue(configFileText, CheckiOStation::class.java)
  }
}