package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.ide.CoreUiCoroutineScopeHolder
import com.intellij.platform.util.coroutines.flow.throttle
import com.intellij.platform.util.progress.createProgressPipe
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.installAndEnableHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.needInstallHyperskillPlugin
import com.jetbrains.edu.learning.stepik.hyperskill.closeDialogAndOpenHyperskillBrowseCourses
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
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import kotlin.coroutines.CoroutineContext

class HyperskillInstallPluginInteractivePanel(parentDisposable: Disposable) : JPanel(), Disposable {
  private var job: Job? = null
  private val progressBarPanel: ProgressBarPanel

  private val modalityContext: CoroutineContext
    get() = ModalityState.stateForComponent(this).asContextElement()

  init {
    Disposer.register(parentDisposable, this)
    isOpaque = false

    layout = CardLayout()

    val installButton = createButtonPanel(EduCoreBundle.message("hyperskill.new.plugin.courses.panel.install.button.text"), true) {
      doInstall()
    }
    add(installButton, INSTALL_BUTTON_ID)

    val openButton = createButtonPanel(EduCoreBundle.message("hyperskill.new.plugin.courses.panel.open.hyperskill.academy.button.text"), false) {
      closeDialogAndOpenHyperskillBrowseCourses(this, modalityContext)
    }
    add(openButton, OPEN_BUTTON_ID)

    progressBarPanel = ProgressBarPanel(CancelPluginActionListener())
    add(progressBarPanel, PROGRESS_BAR_ID)

    showButtonAndUpdateText()
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

      try {
        progressPipe.collectProgressUpdates { installAndEnableHyperskillPlugin(modalityContext) }
      } finally {
        progressUpdater.cancel()
        closeDialogAndOpenHyperskillBrowseCourses(this@HyperskillInstallPluginInteractivePanel, modalityContext)
      }
    }
  }

  private fun createButtonPanel(
    @NlsContexts.Button text: String,
    isDefaultButton: Boolean,
    action: () -> Unit
  ): JPanel {
    return panel {
      row {
        button(text) { action() }.applyToComponent {
          isDefault = isDefaultButton
        }
      }
    }
  }

  private fun showButtonAndUpdateText() {
    val buttonId = if (needInstallHyperskillPlugin()) {
      INSTALL_BUTTON_ID
    }
    else {
      OPEN_BUTTON_ID
    }
    (layout as CardLayout).show(this, buttonId)
  }

  override fun dispose() {
    job?.cancel()
  }

  private inner class CancelPluginActionListener : MouseAdapter() {
    override fun mouseReleased(mouseEvent: MouseEvent?) {
      mouseEvent ?: return
      if (mouseEvent.clickCount == 1 && SwingUtilities.isLeftMouseButton(mouseEvent)) {
        job?.cancel()
        job = null
        showButtonAndUpdateText()
      }
    }
  }

  companion object {
    private const val INSTALL_BUTTON_ID = "INSTALL_BUTTON"
    private const val PROGRESS_BAR_ID = "PROGRESS_BAR"
    private const val OPEN_BUTTON_ID = "OPEN_BUTTON"
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
    size.width = 175
    return size
  }
}