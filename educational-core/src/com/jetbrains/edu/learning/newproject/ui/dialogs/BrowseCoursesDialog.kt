package com.jetbrains.edu.learning.newproject.ui.dialogs

import com.intellij.ide.plugins.DisabledPluginsState
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.platformProviders.CoursesPanelWithTabs
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent
import kotlin.coroutines.CoroutineContext

class BrowseCoursesDialog : OpenCourseDialogBase(), CoroutineScope {
  val panel = CoursesPanelWithTabs(this, disposable)

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = AppUIExecutor.onUiThread(ModalityState.any()).coroutineDispatchingContext() + job

  init {
    title = EduCoreBundle.message("course.dialog.title")
    init()
    rootPane.background = SelectCourseBackgroundColor
    panel.setSidePaneBackground()

    Disposer.register(disposable, Disposable { job.cancel() })
    setupPluginListeners(disposable)
    panel.loadCourses()
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return panel
  }

  private fun setupPluginListeners(disposable: Disposable) {
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(DynamicPluginListener.TOPIC, object : DynamicPluginListener {
      override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        panel.doValidation()
      }
    })
    connection
      // TODO: find out a better way to be notified when plugin installation finishes
      .subscribe(Notifications.TOPIC, object : Notifications {
        override fun notify(notification: Notification) {
          if (notification.groupId == UpdateChecker.getNotificationGroup().displayId) {
            panel.doValidation()
            // TODO: investigate why it leads to IDE freeze when you install python plugin
            // ApplicationManager.getApplication().invokeLater {
            //  PluginManagerConfigurable.shutdownOrRestartApp()
            // }
          }
        }
      })

    val disablePluginListener = Runnable { ApplicationManager.getApplication().invokeLater { panel.doValidation() } }
    Disposer.register(disposable, Disposable {
      @Suppress("UnstableApiUsage")
      DisabledPluginsState.removeDisablePluginListener(disablePluginListener)
    })
    @Suppress("UnstableApiUsage")
    DisabledPluginsState.addDisablePluginListener(disablePluginListener)
  }

  override fun createCenterPanel(): JComponent = panel

  companion object {
    @NonNls
    const val ACTION_PLACE = "COURSE_SELECTION_DIALOG"
  }
}
