package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CourseraSettings", storages = arrayOf(Storage("other.xml")))
class CourseraSettings : PersistentStateComponent<CourseraSettings> {
  var email: String? = null
  var token: String? = null

  override fun getState(): CourseraSettings? = this

  override fun loadState(state: CourseraSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  companion object {
    @JvmStatic
    fun getInstance(): CourseraSettings = ServiceManager.getService(CourseraSettings::class.java)
  }
}