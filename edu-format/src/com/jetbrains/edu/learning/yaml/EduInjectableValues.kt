package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.ObjectMapper

class InjectableValueKey<T>(val name: String)

/**
 * Injectable values are custom key-value parameters associated with an [ObjectMapper].
 * A value is assigned to an [ObjectMapper] with a [setEduValue] method.
 * It can later be read in two ways:
 *  - with a [ObjectMapper.getEduValue] method;
 *  - with a [JacksonInject] annotation for a builder, that deserialized a value from YAML.
 *    Normally, a builder has access only to data written in YAML, but it can also use values injected into an [ObjectMapper].
 */
private class EduInjectableValues(val values: MutableMap<String, Any?>): InjectableValues.Std(values)

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