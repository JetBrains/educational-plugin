package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.coursecreator.actions.mixins.StudyItemDeserializer
import com.jetbrains.edu.learning.courseFormat.tasks.Task
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
  mapper.registerModule(module)
  mapper
}

fun <T : StudyItem, K : T> T.copyAs(clazz: Class<K>): K {
  try {
    val jsonText = MAPPER.writeValueAsString(this)
    val copy = MAPPER.readValue(jsonText, clazz)
    copy.init(null, parent, true)
    return copy
  }
  catch (e: JsonProcessingException) {
    LOG.error("Failed to create study item copy: $javaClass as $clazz", e)
  }
  error("Failed to create study item copy: $javaClass as $clazz")
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
