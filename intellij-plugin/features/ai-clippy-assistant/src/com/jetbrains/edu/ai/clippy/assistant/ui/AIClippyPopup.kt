package com.jetbrains.edu.ai.clippy.assistant.ui

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.IntelliJSpacingConfiguration
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.clippy.assistant.AIClippyService.ClippyLinkAction
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import java.awt.Point
import java.awt.Toolkit
import javax.swing.JComponent

class AIClippyPopup {
  private val popup: JBPopup
  private var text = ""
  private val linkActions = mutableListOf<ClippyLinkAction>()

  private var contentPanel: Wrapper

  val isDisposed: Boolean
    get() = popup.isDisposed

  val isVisible: Boolean
    get() = popup.isVisible

  init {
    contentPanel = Wrapper(createContent())
    popup = createPopup(contentPanel)
  }

  @RequiresEdt
  fun show(project: Project) {
    require (!popup.isDisposed) { "Clippy is disposed" }
    if (isVisible) return

    val window = WindowManager.getInstance().getIdeFrame(project) ?: error("No IDE frame found")
    val component = window.component

    val bottomRightPoint = getPoint()
    val relativePoint = RelativePoint(component, bottomRightPoint)
    popup.show(relativePoint)
  }

  @RequiresEdt
  fun updateText(newText: String) {
    text = newText

    updatePanel()
  }

  @RequiresEdt
  fun updateLinkActions(linkActions: List<ClippyLinkAction>) {
    this.linkActions.clear()
    linkActions.forEach { this.linkActions.add(it) }

    updatePanel()
  }

  @RequiresEdt
  fun updatePanel() {
    contentPanel.setContent(createContent())
    popup.content.revalidate()
    popup.content.repaint()
    popup.pack(true, true)
  }

  private fun createContent(): JComponent {
    val image = ScaledImageLabel(EduAiClippyImages.Clippy)
    val panel = panel {
      customizeSpacingConfiguration(object : IntelliJSpacingConfiguration() {
        override val horizontalColumnsGap: Int = 5
      }) {
        row {
          cell(image).align(Align.CENTER)
          cell(createCongratulationsPanel()).align(Align.FILL)
        }
      }
    }.apply {
      border = JBUI.Borders.empty(0, 5, 0, 12)
    }
    return panel
  }

  private fun createCongratulationsPanel(): JComponent {
    return panel {
      row {
        text(text, maxLineLength = 35).align(AlignY.FILL)
      }
      row {
        for (link in linkActions) {
          link(link.name) { link.action.invoke() }
        }
        cell()
      }
    }
  }

  private fun createPopup(content: JComponent): JBPopup {
    val button = IconButton(
      CommonBundle.message("action.text.close"),
      AllIcons.Actions.Close,
      AllIcons.Actions.CloseHovered
    )
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(content, content)
      .setTitle(EduAIClippyAssistantBundle.message("settings.ai.clippy.assistant"))
      .setShowBorder(false)
      .setShowShadow(false)
      .setMovable(true)
      .setCancelKeyEnabled(true)
      .setCancelButton(button)
      .setCancelOnClickOutside(false)
      .setCancelOnWindowDeactivation(true)
      .setFocusable(true)
      .setRequestFocus(true)
      .createPopup()
    return popup
  }

  private fun getPoint(): Point {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val xOffset = JBUIScale.scale(1000)
    val yOffset = JBUIScale.scale(500)
    return Point(screenSize.width - xOffset, screenSize.height - yOffset)
  }
}