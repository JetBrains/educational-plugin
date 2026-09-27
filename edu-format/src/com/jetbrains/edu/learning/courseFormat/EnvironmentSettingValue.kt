package com.jetbrains.edu.learning.courseFormat

enum class EnvironmentSettingValueType {
  /**
   * The value may be set by the teacher, and the course archive will be created using their specified value.
   */
  USER_DEFINED,

  /**
   * The value depends on the course content, and the teacher cannot modify it.
   */
  COURSE_DEFINED
}

data class EnvironmentSettingValue(val value: String, val valueType: EnvironmentSettingValueType) {
  companion object {
    fun user(value: String): EnvironmentSettingValue = EnvironmentSettingValue(value, EnvironmentSettingValueType.USER_DEFINED)
    fun course(value: String): EnvironmentSettingValue = EnvironmentSettingValue(value, EnvironmentSettingValueType.COURSE_DEFINED)
  }
}

typealias DefaultEnvironmentSettings = Map<EnvironmentSettingKey, EnvironmentSettingValue>
typealias EnvironmentSettings = Map<String, String>

fun EnvironmentSettings.with(default: DefaultEnvironmentSettings): EnvironmentSettings {
  val result = this.toMutableMap()

  for ((key, settingValue) in default) {
    when (settingValue.valueType) {
      EnvironmentSettingValueType.USER_DEFINED -> {
        // USER_DEFINED: prefer value from `this` if exists, otherwise use default
        if (key.name !in result) {
          result[key.name] = settingValue.value
        }
      }

      EnvironmentSettingValueType.COURSE_DEFINED -> {
        // COURSE_DEFINED: always use value from default
        result[key.name] = settingValue.value
      }
    }
  }

  return result
}

fun DefaultEnvironmentSettings.with(vararg keyValue: Pair<EnvironmentSettingKey, EnvironmentSettingValue?>): DefaultEnvironmentSettings {
  val result = toMutableMap()

  for ((key, value) in keyValue) {
    if (value != null) {
      result += (key to value)
    }

  }
  return result
}

fun defaultEnvironmentSettings(vararg settings: Pair<EnvironmentSettingKey, EnvironmentSettingValue?>): DefaultEnvironmentSettings {
  return emptyMap<EnvironmentSettingKey, EnvironmentSettingValue>().with(*settings)
}