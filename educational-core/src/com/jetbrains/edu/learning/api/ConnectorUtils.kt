package com.jetbrains.edu.learning.api

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule

object ConnectorUtils {
  @JvmStatic
  fun createMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
    objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
    objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
    objectMapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    objectMapper.disable(MapperFeature.AUTO_DETECT_SETTERS)
    return objectMapper
  }

  @JvmStatic
  fun createRegisteredMapper(module: SimpleModule): ObjectMapper {
    val objectMapper = createMapper()
    objectMapper.registerModule(module)
    return objectMapper
  }
}