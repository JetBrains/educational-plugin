package com.jetbrains.edu.jbserver

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task


class SectionSerializer(t: Class<Section>? = null) : StdSerializer<Section>(t) {

  override fun serialize(value: Section, generator: JsonGenerator, provider: SerializerProvider) = generator.run {
    if (value.id != 0)
      writeNumberField(ID_FIELD, value.id)
    if (value.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE) {
      writeStringField(TITLE_FIELD, value.name)
      writeArrayFieldStart(ITEMS_FILED)
      value.items.forEach {
        writeObject(it)
      }
      writeEndArray()
    }
  }

  override fun serializeWithType(value: Section, generator: JsonGenerator, serializers: SerializerProvider, typeSerializer: TypeSerializer) {
    val typeId = typeSerializer.typeId(value, JsonToken.START_OBJECT)
    typeSerializer.writeTypePrefix(generator, typeId)
    serialize(value, generator, serializers)
    typeSerializer.writeTypeSuffix(generator, typeId)
  }

}


class LessonSerializer(t: Class<Lesson>? = null) : StdSerializer<Lesson>(t) {

  override fun serialize(value: Lesson, generator: JsonGenerator, provider: SerializerProvider) = generator.run {
    if (value.id != 0)
      writeNumberField(ID_FIELD, value.id)
    if (value.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE) {
      writeStringField(TITLE_FIELD, value.name)
      writeArrayFieldStart(ITEMS_FILED)
      value.taskList.forEach {
        writeObject(it)
      }
      writeEndArray()
    }
  }

  override fun serializeWithType(value: Lesson, generator: JsonGenerator, serializers: SerializerProvider, typeSerializer: TypeSerializer) {
    val typeId = typeSerializer.typeId(value, JsonToken.START_OBJECT)
    typeSerializer.writeTypePrefix(generator, typeId)
    serialize(value, generator, serializers)
    typeSerializer.writeTypeSuffix(generator, typeId)
  }

}


class TaskSerializer(t: Class<Task>? = null) : StdSerializer<Task>(t) {

  override fun serialize(value: Task, generator: JsonGenerator, provider: SerializerProvider) = generator.run {
    if (value.id != 0)
      writeNumberField(ID_FIELD, value.stepId)
    if (value.stepikChangeStatus != StepikChangeStatus.UP_TO_DATE) {
      writeStringField(TITLE_FIELD, value.name)
      writeStringField(DESCRIPTION_TEXT_FIELD, value.descriptionText)
      writeObjectField(DESCRIPTION_FORMAT_FIELD, value.descriptionFormat)
      writeObjectField(TASK_FILES_FIELD, value.taskFiles)
      writeObjectField(TEST_FILES_FIELD, value.testsText)
      // todo : handle this by standard serializer if possible
    }
  }

  override fun serializeWithType(value: Task, generator: JsonGenerator, serializers: SerializerProvider, typeSerializer: TypeSerializer) {
    val typeId = typeSerializer.typeId(value, JsonToken.START_OBJECT)
    typeSerializer.writeTypePrefix(generator, typeId)
    serialize(value, generator, serializers)
    typeSerializer.writeTypeSuffix(generator, typeId)
  }

}
