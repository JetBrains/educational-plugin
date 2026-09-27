package com.jetbrains.edu.learning.courseFormat

@JvmInline
value class EnvironmentSettingKey(val name: String)

fun Course.getEnvironmentSetting(key: EnvironmentSettingKey): String? = environmentSettings[key.name]

fun Course.setEnvironmentSetting(key: EnvironmentSettingKey, value: String?) {
  if (value != null) {
    environmentSettings += key.name to value
  }
  else {
    environmentSettings -= key.name
  }
}