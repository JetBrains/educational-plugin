@file:JvmName("LessonYamlUtil")

package com.jetbrains.edu.coursecreator.configuration.mixins

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.jetbrains.edu.coursecreator.configuration.InvalidYamlFormatException
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task

private const val CONTENT = "content"
private const val TYPE = "content"

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE,
                isGetterVisibility = JsonAutoDetect.Visibility.NONE,
                fieldVisibility = JsonAutoDetect.Visibility.NONE)
@JsonDeserialize(builder = LessonBuilder::class)
@JsonSerialize(using = LessonSerializer::class)
abstract class LessonYamlMixin {
  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var taskList: List<Task>
}

@JsonPOJOBuilder(withPrefix = "")
private class LessonBuilder(@JsonProperty(CONTENT) val content: List<String?>) {
  @Suppress("unused") //used for deserialization
  private fun build(): Lesson {
    val lesson = Lesson()
    val items = content.map {
      if (it == null) {
        throw InvalidYamlFormatException("Unnamed item")
      }
      TaskWithType(it)
    }
    lesson.updateTaskList(items)
    return lesson
  }
}

private class LessonSerializer : StdSerializer<Lesson>(Lesson::class.java){
  override fun serialize(value: Lesson?, gen: JsonGenerator?, provider: SerializerProvider?) {
    if (value == null || gen == null || provider == null) {
      return
    }

    gen.writeStartObject()
    gen.writeObjectField(TYPE, if (value is FrameworkLesson) "framework" else "plain")
    val type = provider.constructType(Lesson::class.java)
    val beanDescription = provider.config.introspect<BeanDescription>(type)
    val beanSerializer = BeanSerializerFactory.instance.findBeanSerializer(provider, type, beanDescription)
    beanSerializer.unwrappingSerializer(null).serialize(value, gen, provider)
    gen.writeEndObject()
  }
}
