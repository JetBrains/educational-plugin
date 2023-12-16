package com.jetbrains.edu.shell

import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.sh.ShFileType
import com.intellij.sh.ShLanguage
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.shell.messages.EduShellBundle
import org.jetbrains.annotations.NonNls
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function
import javax.swing.JComponent

class ShellWslExistenceNotificationProvider : EditorNotificationProvider, DumbAware {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (PropertiesComponent.getInstance(project).getBoolean(DISABLE_INSTALL_WSL_NOTIFICATION, false)) return null
    if (project.course?.languageById != ShLanguage.INSTANCE || file.fileType !is ShFileType) return null
    if (!SystemInfo.isWindows) return null

    val cachedDistributions = WslDistributionManager.getInstance().cachedInstalledDistributions
    when {
      cachedDistributions == null -> {
        // It means that no check have been done yet or results are outdated, let's make one
        val retrieverService = WslDistributionsRetrieverService.getInstance(project)
        if (!retrieverService.isInProcess) {
          retrieverService.retrieve()
        }
      }
      cachedDistributions.isEmpty() -> {
        // It means we have already made check, but there is no WSL installed
        return Function {
          EditorNotificationPanel().apply {
            text = EduShellBundle.message("install.wsl.notification.text")
            createActionLabel(EduShellBundle.message("install.wsl.notification.action")) {
              EduBrowser.getInstance().browse(WSL_CONFIGURE_DOCUMENTATION_LINK)
            }
            createActionLabel(EduShellBundle.message("install.wsl.notification.disable")) {
              PropertiesComponent.getInstance(project).setValue(DISABLE_INSTALL_WSL_NOTIFICATION, true)
              isVisible = false
            }
          }
        }
      }
    }
    return null
  }

  @Service(Service.Level.PROJECT)
  private class WslDistributionsRetrieverService(private val project: Project) {
    private val isBusy = AtomicBoolean(false)

    val isInProcess: Boolean
      get() = isBusy.get()

    fun retrieve() {
      if (!lock()) return
      WslDistributionManager.getInstance().installedDistributionsFuture.thenApply {
        try {
          runReadAction {
            if (project.isDisposed) return@runReadAction
            EditorNotifications.getInstance(project).updateAllNotifications()
          }
        }
        finally {
          unlock()
        }
      }
    }

    private fun lock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    private fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): WslDistributionsRetrieverService = project.service()
    }
  }

  companion object {
    @NonNls
    private const val DISABLE_INSTALL_WSL_NOTIFICATION = "Edu.DisableWslNotification"
  }
}