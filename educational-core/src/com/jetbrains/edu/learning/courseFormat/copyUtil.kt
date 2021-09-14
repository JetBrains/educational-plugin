package com.jetbrains.edu.learning.courseFormat

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils

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
