package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class SectionSerializer(t: Class<Section>? = null) : StdSerializer<Section>(t) {

  override fun serialize(value: Section, gen: JsonGenerator, provider: SerializerProvider) = gen.run {
    if (value.id != 0) {
      writeNumberField("id", value.id)
    }
    else {
      writeStringField("title", value.name)
      writeArrayFieldStart("items")
      value.items.forEach {
        writeObject(it)
      }
      writeEndArray()
    }
  }

  override fun serializeWithType(value: Section, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
    val typeId = typeSer.typeId(value, JsonToken.START_OBJECT)
    typeSer.writeTypePrefix(gen, typeId)
    serialize(value, gen, serializers)
    typeSer.writeTypeSuffix(gen, typeId)
  }

}


class LessonSerializer(t: Class<Lesson>? = null) : StdSerializer<Lesson>(t) {

  override fun serialize(value: Lesson, gen: JsonGenerator, provider: SerializerProvider) = gen.run {
    if (value.id != 0) {
      writeNumberField("id", value.id)
    }
    else {
      writeStringField("title", value.name)
      writeArrayFieldStart("items")
      value.taskList.forEach {
        writeObject(it)
      }
      writeEndArray()
    }
  }

  override fun serializeWithType(value: Lesson, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
    val typeId = typeSer.typeId(value, JsonToken.START_OBJECT)
    typeSer.writeTypePrefix(gen, typeId)
    serialize(value, gen, serializers)
    typeSer.writeTypeSuffix(gen, typeId)
  }

}


class TaskSerializer(t: Class<Task>? = null) : StdSerializer<Task>(t) {

  override fun serialize(value: Task, gen: JsonGenerator, provider: SerializerProvider) = gen.run {
    if (value.stepId == 0) {
      writeStringField("title", value.name)
      writeStringField("description", value.descriptionText)
      writeObjectField("descriptionFormat", value.descriptionFormat)
      writeObjectField("task_files", value.taskFiles)
      writeObjectField("test_files", value.testsText)
      // todo : handle this by standard serializer if possible
    }
    else
      writeNumberField("id", value.stepId)
  }

  override fun serializeWithType(value: Task, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
    val typeId = typeSer.typeId(value, JsonToken.START_OBJECT)
    typeSer.writeTypePrefix(gen, typeId)
    serialize(value, gen, serializers)
    typeSer.writeTypeSuffix(gen, typeId)
  }

}
