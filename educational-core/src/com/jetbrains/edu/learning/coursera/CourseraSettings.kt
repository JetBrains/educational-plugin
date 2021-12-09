package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CourseraSettings", storages = [Storage("other.xml", roamingType = RoamingType.DISABLED)])
class CourseraSettings : PersistentStateComponent<CourseraSettings> {
  var email: String = ""

  override fun getState(): CourseraSettings = this

  override fun loadState(state: CourseraSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    @JvmStatic
    fun getInstance(): CourseraSettings = service()
  }
}