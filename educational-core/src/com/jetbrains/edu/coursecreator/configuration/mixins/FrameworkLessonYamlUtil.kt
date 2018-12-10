package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val TYPE = "type"
private const val CONTENT = "content"

@JsonSerialize(using = FrameworkLessonSerializer::class)
@JsonDeserialize(builder = FrameworkLessonBuilder::class)
abstract class FrameworkLessonYamlUtil : LessonYamlMixin() {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var taskList: List<Task>
}

// adding framework lesson type property
private class FrameworkLessonSerializer : StdSerializer<FrameworkLesson>(FrameworkLesson::class.java) {
  override fun serialize(value: FrameworkLesson, gen: JsonGenerator, provider: SerializerProvider) {
    gen.writeStartObject()
    gen.writeObjectField(TYPE, "framework")
    val type = provider.constructType(Lesson::class.java)
    val beanDescription = provider.config.introspect<BeanDescription>(type)
    val beanSerializer = BeanSerializerFactory.instance.findBeanSerializer(provider, type, beanDescription)
    beanSerializer.unwrappingSerializer(null).serialize(value, gen, provider)
    gen.writeEndObject()
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class FrameworkLessonBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): FrameworkLesson {
    val lesson = FrameworkLesson()
    val items = parseTaskList(content)
    lesson.updateTaskList(items)
    return lesson
  }
}
