package com.jetbrains.edu.ai.translation.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Converter.Factory
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.lang.reflect.Type

class TranslationConverterFactory : Factory() {
  private val objectMapper = jacksonObjectMapper()
  private val factory = JacksonConverterFactory.create(objectMapper)

  override fun responseBodyConverter(type: Type, annotations: Array<out Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
    val delegateConverter = factory.responseBodyConverter(type, annotations, retrofit) ?: return null
    return Converter<ResponseBody, Any> { body ->
      if (body.contentLength() == 0L) null else delegateConverter.convert(body)
    }
  }
}