package com.jetbrains.edu.python.learning

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.util.PlatformUtils
import com.intellij.xml.util.XmlStringUtil
import com.jetbrains.edu.python.learning.messages.EduPythonBundle.message

class PyShowTutorialStartupActivity : StartupActivity {

  override fun runActivity(project: Project) {
    if (PropertiesComponent.getInstance().isValueSet(POPUP_SHOWN) || !PlatformUtils.isPyCharmEducational()) return

    Notification(
      "EduTools",
      message("watch.tutorials.title"),
      XmlStringUtil.wrapInHtml(message("watch.tutorials.message.html")),
      NotificationType.INFORMATION
    )
      .setListener(NotificationListener.UrlOpeningListener(true))
      .notify(null)

    PropertiesComponent.getInstance().setValue(POPUP_SHOWN, true)
  }

  companion object {
    private const val POPUP_SHOWN = "StudyShowPopup"
  }
}
