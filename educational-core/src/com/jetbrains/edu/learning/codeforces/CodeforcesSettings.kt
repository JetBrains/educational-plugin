package com.jetbrains.edu.learning.codeforces

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.util.xmlb.XmlSerializerUtil
import com.jetbrains.edu.learning.messages.EduCoreBundle

@State(name = "CodeforcesSettings", storages = [Storage("other.xml")])
class CodeforcesSettings : PersistentStateComponent<CodeforcesSettings> {
  var preferableTaskTextLanguage: TaskTextLanguage? = null
  var preferableLanguage: String? = null
  var doNotShowLanguageDialog: Boolean = false

  override fun getState(): CodeforcesSettings? = this

  override fun loadState(state: CodeforcesSettings) {
    XmlSerializerUtil.copyBean(state, this)
  }

  fun isSet(): Boolean = preferableLanguage != null && preferableTaskTextLanguage != null

  companion object {
    val codeforcesNotificationGroup = NotificationGroup(
      EduCoreBundle.message("codeforces.information", CodeforcesNames.CODEFORCES_TITLE),
      NotificationDisplayType.BALLOON,
      true
    )

    @JvmStatic
    fun getInstance(): CodeforcesSettings = service()
  }
}