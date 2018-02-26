package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduSettings

class UserSettingObserver(private vararg val postLoginActions: Runnable) {

  private var myBusConnection: MessageBusConnection? = null

  fun observe() {
    if (myBusConnection != null) {
      myBusConnection!!.disconnect()
    }
    myBusConnection = ApplicationManager.getApplication().messageBus.connect()
    myBusConnection!!.subscribe<EduSettings.StudySettingsListener>(EduSettings.SETTINGS_CHANGED, userSettingListener())

  }

  private fun userSettingListener(): EduSettings.StudySettingsListener {
    return EduSettings.StudySettingsListener {
      val user = EduSettings.getInstance().user
      if (user != null) {
        ApplicationManager.getApplication().invokeLater(
          {
            for (action in postLoginActions) {
              action.run()
            }
            myBusConnection!!.disconnect()
            myBusConnection = null
          },
          ModalityState.any())
      }
    }
  }
}