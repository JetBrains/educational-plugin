@file:JvmName("CopyUtils")

package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.json.mixins.StudyItemDeserializer
import com.jetbrains.edu.learning.serialization.SerializationUtils

private val LOG = logger<StudyItem>()

private val MAPPER: ObjectMapper by lazy {
  val mapper = ObjectMapper()
  mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
  mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
  mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
  mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)

  val module = SimpleModule()
  module.addSerializer(StudyItem::class.java, StudyItemCopySerializer())
  module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())
  // For FileContents, serialize only whether it is binary or textual:
  module.addSerializer(FileContents::class.java, FileContentsSerializer())
  module.addDeserializer(FileContents::class.java, FileContentsDeserializer())
  mapper.registerModule(module)

  mapper
}

fun <T : StudyItem> T.copy(): T {
  try {
    val jsonText = MAPPER.writeValueAsString(this)
    val copy = MAPPER.readValue(jsonText, javaClass)
    copy.init(parent, true)
    return copy
  }
  catch (e: JsonProcessingException) {
    LOG.error("Failed to create study item copy", e)
  }
  error("Failed to create study item copy")
}

fun <T : StudyItem> copyFileContents(sourceItem: T, destinationItem: T) {
  if (sourceItem is Task) {
    copyFileContentsForTasks(sourceItem, destinationItem as Task)
    return
  }
  if (sourceItem !is ItemContainer) return
  destinationItem as ItemContainer

  for (subItem1 in sourceItem.items) {
    val subItem2 = destinationItem.getItem(subItem1.name) ?: continue
    copyFileContents(subItem1, subItem2)
  }
}

fun copyFileContentsForTasks(sourceTask: Task, destinationTask: Task) {
  for (taskFile1 in sourceTask.taskFiles.values) {
    val taskFile2 = destinationTask.getTaskFile(taskFile1.name)
    taskFile2?.contents = taskFile1.contents
  }
}

class StudyItemCopySerializer : JsonSerializer<StudyItem>() {
  override fun serialize(value: StudyItem, jgen: JsonGenerator, provider: SerializerProvider) {
    jgen.writeStartObject()
    val javaType = provider.constructType(value::class.java)
    val beanDesc: BeanDescription = provider.config.introspect(javaType)
    val serializer: JsonSerializer<Any> =
      BeanSerializerFactory.instance.findBeanOrAddOnSerializer(provider, javaType, beanDesc,
                                                               provider.isEnabled(MapperFeature.USE_STATIC_TYPING))
    serializer.unwrappingSerializer(null).serialize(value, jgen, provider)
    if (value !is Course) {
      addItemType(value, jgen)
    }

    jgen.writeEndObject()
  }

  private fun addItemType(value: StudyItem, jgen: JsonGenerator) {
    val fieldName =
      if (value is Task) {
        SerializationUtils.Json.TASK_TYPE
      }
      else {
        SerializationUtils.Json.ITEM_TYPE
      }
    jgen.writeObjectField(fieldName, value.itemType)
  }
}

class FileContentsSerializer : JsonSerializer<FileContents>() {
  override fun serialize(value: FileContents?, jgen: JsonGenerator, provider: SerializerProvider) {
    val isBinary = when (value) {
      is BinaryContents -> true
      is TextualContents -> false
      else -> null
    }
    jgen.writeStartObject()
    if (isBinary != null) {
      jgen.writeBooleanField("is_binary", isBinary)
    }
    jgen.writeEndObject()
  }
}

class FileContentsDeserializer : JsonDeserializer<FileContents>() {
  override fun deserialize(parser: JsonParser, context: DeserializationContext): FileContents {
    val contentsObject = parser.codec.readTree<JsonNode>(parser)
    val isBinary = contentsObject?.get("is_binary")?.asBoolean()
    return when (isBinary) {
      true -> BinaryContents.EMPTY
      false -> TextualContents.EMPTY
      else -> UndeterminedContents.EMPTY
    }
  }
}