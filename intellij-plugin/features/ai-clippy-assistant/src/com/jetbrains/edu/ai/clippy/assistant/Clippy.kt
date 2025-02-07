package com.jetbrains.edu.ai.clippy.assistant

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.clippy.assistant.action.ShowDiffClippyComments
import com.jetbrains.edu.ai.clippy.assistant.messages.EduAIClippyAssistantBundle
import com.jetbrains.edu.ai.clippy.assistant.ui.EduAiClippyImages
import com.jetbrains.edu.ai.clippy.assistant.ui.ScaledImageLabel
import java.awt.Component
import java.awt.Font
import java.awt.Point
import java.awt.Toolkit
import javax.swing.JComponent
import javax.swing.JPanel

class Clippy(private val project: Project) {
  private val popup: JBPopup
  private val text = AtomicProperty("")

  val isDisposed: Boolean
    get() = popup.isDisposed

  val isVisible: Boolean
    get() = popup.isVisible

  init {
    val content = createContent()
    popup = createPopup(content)
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
    text.set(newText)

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
    }
    applyFontRecursively(panel, JBUI.Fonts.label(16f))
    return panel
  }

  private fun createCongratulationsPanel(): JComponent {
    return panel {
      row {
        text("", maxLineLength = 35).align(AlignY.FILL).bindText(text)
      }
      row {
        link(EduAIClippyAssistantBundle.message("clippy.diff.action.show")) {
          val action = ActionManager.getInstance().getAction(ShowDiffClippyComments.ACTION_ID) ?: return@link
          val dataContext = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .build()
          ActionUtil.invokeAction(action, dataContext, "", null, null)
        }
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

  private fun applyFontRecursively(component: Component, font: Font) {
    component.font = font
    if (component is JPanel) {
      for (child in component.components) {
        applyFontRecursively(child, font)
      }
    }
  }
}