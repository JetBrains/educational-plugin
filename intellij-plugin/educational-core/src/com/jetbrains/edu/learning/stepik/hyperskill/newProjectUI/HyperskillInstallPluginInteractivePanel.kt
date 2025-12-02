package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.platform.ide.CoreUiCoroutineScopeHolder
import com.intellij.platform.util.coroutines.flow.throttle
import com.intellij.platform.util.progress.createProgressPipe
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.launchOnShow
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.installAndEnableHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.needInstallHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.openHyperskillBrowseCourses
import com.jetbrains.edu.learning.ui.isDefault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

class HyperskillInstallPluginInteractivePanel(parentDisposable: Disposable) : JPanel(), Disposable {
  private var job: Job? = null
  private val progressBarPanel: ProgressBarPanel
  private lateinit var actionButton: JButton

  private val modalityContext: CoroutineContext
    get() = ModalityState.stateForComponent(this).asContextElement()

  init {
    Disposer.register(parentDisposable, this)
    isOpaque = false

    layout = CardLayout()

    val buttonText = if (needInstallHyperskillPlugin()) {
      EduCoreBundle.message("hyperskill.new.plugin.courses.panel.install.button.text")
    }
    else {
      EduCoreBundle.message("hyperskill.new.plugin.courses.panel.open.hyperskill.academy.button.text")
    }

    val button = panel {
      row {
        button(buttonText) { doButtonAction() }.applyToComponent {
          isDefault = true
          actionButton = this
        }
      }
    }

    add(button, BUTTON_ID)

    progressBarPanel = ProgressBarPanel(CancelPluginActionListener())
    add(progressBarPanel, PROGRESS_BAR_ID)

    showButton()

    launchOnShow("EduHyperskillInstallButtonPanelRequestFocus", context = modalityContext) {
      actionButton.requestFocusInWindow()
    }
  }

  private fun doInstall() {
    (layout as CardLayout).show(this, PROGRESS_BAR_ID)

    job = service<CoreUiCoroutineScopeHolder>().coroutineScope.launch {
      val progressPipe = createProgressPipe()

      val progressUpdater = launch {
        progressPipe.progressUpdates().throttle(50).collect {
          val fraction = it.fraction ?: 0.0
          withContext(Dispatchers.EDT + modalityContext) {
            progressBarPanel.updateProgressBar(fraction)
          }
        }
      }

      val hasOpenProjects = ProjectManager.getInstance().openProjects.any { !it.isDisposed }

      try {
        progressPipe.collectProgressUpdates { installAndEnableHyperskillPlugin(modalityContext, !hasOpenProjects) }
      }
      finally {
        progressUpdater.cancel()
        closeDialogAndOpenHyperskillBrowseCourses()
      }
    }
  }

  override fun dispose() {
    job?.cancel()
  }

  private fun showButton() {
    (layout as CardLayout).show(this, BUTTON_ID)
  }

  private fun doButtonAction() {
    if (needInstallHyperskillPlugin()) {
      doInstall()
    }
    else {
      closeDialogAndOpenHyperskillBrowseCourses()
    }
  }

  private fun closeDialogAndOpenHyperskillBrowseCourses() {
    service<CoreUiCoroutineScopeHolder>().coroutineScope.launch(Dispatchers.EDT + modalityContext) {
      DialogWrapper.findInstance(this@HyperskillInstallPluginInteractivePanel)?.close(DialogWrapper.OK_EXIT_CODE)
      openHyperskillBrowseCourses()
    }
  }

  private inner class CancelPluginActionListener : MouseAdapter() {
    override fun mouseReleased(mouseEvent: MouseEvent?) {
      mouseEvent ?: return
      if (mouseEvent.clickCount == 1 && SwingUtilities.isLeftMouseButton(mouseEvent)) {
        job?.cancel()
        job = null
        showButton()
      }
    }
  }

  companion object {
    private const val BUTTON_ID = "BUTTON"
    private const val PROGRESS_BAR_ID = "PROGRESS_BAR"
  }
}

// copied from `com.intellij.openapi.wm.impl.welcomeScreen.learnIde.jbAcademy.ProgressBarPanel`
private class ProgressBarPanel(listener: MouseAdapter) : JPanel() {
  val projectCancelButton = JLabel(AllIcons.Actions.DeleteTag).apply {
    border = JBUI.Borders.empty(0, 8, 0, 14)
  }

  private val progressBar = JProgressBar().apply {
    isOpaque = false
    isIndeterminate = false
  }

  init {
    isOpaque = false
    layout = BorderLayout()
    add(progressBar, BorderLayout.CENTER)

    val buttonWrapper = Wrapper().apply {
      addMouseListener(listener)
      addMouseMotionListener(listener)
      setContent(projectCancelButton)
    }
    add(buttonWrapper, BorderLayout.EAST)
  }

  fun updateProgressBar(fraction: Double) {
    progressBar.value = (fraction * 100).toInt()
  }

  override fun getPreferredSize(): Dimension {
    val size = super.getPreferredSize()
    size.width = JBUI.scale(175)
    return size
  }
}