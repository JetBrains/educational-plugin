package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper
import com.jetbrains.edu.learning.courseFormat.EduFile

typealias AdditionalFilesProvider = (courseType: String, environment: String, languageId: String) -> List<EduFile>

abstract class InjectableValueKey<T>(val name: String)

private class EduInjectableValues(internal val values: MutableMap<String, Any?>): InjectableValues.Std(values)

@Suppress("UNCHECKED_CAST")
fun <T> ObjectMapper.getEduValue(key: InjectableValueKey<T>): T? {
  val values = injectableValues as? EduInjectableValues
  return values?.values?.get(key.name) as? T
}

fun <T> ObjectMapper.setEduValue(key: InjectableValueKey<T>, value: T?) {
  var values = injectableValues as? EduInjectableValues
  if (values == null) {
    values = EduInjectableValues(mutableMapOf())
    injectableValues = values
  }

  values.addValue(key.name, value)
}