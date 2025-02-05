package com.jetbrains.edu.smartSearch.connector

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.lang.reflect.Type

class SmartSearchConverterFactory(private val objectMapper: ObjectMapper = jacksonObjectMapper()) : Converter.Factory() {
  override fun responseBodyConverter(
    type: Type,
    annotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<ResponseBody, List<SmartSearchService.CourseTaskData>> {
    return Converter<ResponseBody, List<SmartSearchService.CourseTaskData>> { body ->
      body.byteStream().use { resource ->
        val rootNode = objectMapper.readTree(resource)
        val metadatasNode = rootNode.get("metadatas")

        metadatasNode?.firstOrNull()?.let { metadataArray ->
          metadataArray.mapNotNull { metadataNode ->
            try {
              objectMapper.treeToValue(metadataNode, SmartSearchService.CourseTaskData::class.java)
            } catch (e: Exception) {
              null
            }
          }
        } ?: emptyList()
      }
    }
  }

  override fun requestBodyConverter(
    type: Type,
    parameterAnnotations: Array<out Annotation>,
    methodAnnotations: Array<out Annotation>,
    retrofit: Retrofit
  ): Converter<*, RequestBody>? {
    return JacksonConverterFactory.create(objectMapper).requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
  }
}