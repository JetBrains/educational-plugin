@file:JvmName("SwingTaskUtil")

package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.getIconPath
import com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels.ChoiceTaskSpecificPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels.SortingBasedTaskSpecificPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels.TableTaskSpecificPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager.INTELLIJ_ICON_QUICKFIX_OFF_BULB
import com.jetbrains.edu.learning.xmlEscaped
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.nodes.Element
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.JToggleButton
import javax.swing.border.Border
import javax.swing.text.html.HTMLEditorKit
import kotlin.math.roundToInt

fun createSpecificPanel(task: Task?): JPanel? {
  return when (task) {
    is ChoiceTask -> ChoiceTaskSpecificPanel(task)
    is SortingBasedTask -> SortingBasedTaskSpecificPanel(task)
    is TableTask -> TableTaskSpecificPanel(task)
    else -> null
  }
}

fun createTextPane(editorKit: HTMLEditorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()): JTextPane {
  prepareCss(editorKit)

  val textPane = object : JTextPane() {
    override fun getSelectedText(): String {
      // see EDU-3185
      return super.getSelectedText().replace(Typography.nbsp, ' ')
    }
  }

  textPane.contentType = editorKit.contentType
  textPane.editorKit = editorKit
  textPane.isEditable = false
  textPane.background = TaskToolWindowView.getTaskDescriptionBackgroundColor()

  return textPane
}

private fun prepareCss(editorKit: HTMLEditorKit) {
  // ul padding of JBHtmlEditorKit is too small, so copy-pasted the style from
  // com.intellij.codeInsight.documentation.DocumentationComponent.prepareCSS
  editorKit.styleSheet.addRule("ul { padding: 3px 16px 0 0; }")
  editorKit.styleSheet.addRule("li { padding: 3px 0 4px 5px; }")
  editorKit.styleSheet.addRule(".hint { padding: 17px 0 16px 0; }")
}

const val HINT_PROTOCOL = "hint://"
const val CHEVRON_RIGHT = "&#8250"
const val CHEVRON_DOWN = "&#8964"

private val LOG = Logger.getInstance(SwingToolWindow::class.java)  //TODO we probably need another logger here
private const val DEFAULT_ICON_SIZE = 16

@VisibleForTesting
fun getHintIconSize(): Int {
  val currentFontSize = UISettings.getInstance().fontSize
  val defaultFontSize = FontPreferences.DEFAULT_FONT_SIZE
  return (DEFAULT_ICON_SIZE * currentFontSize / defaultFontSize.toFloat()).roundToInt()
}

fun wrapHintSwing(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val bulbWithTheme = getIconPath(INTELLIJ_ICON_QUICKFIX_OFF_BULB.trimStart('/'))
  val bulbIcon = if (!isUnitTestMode) SwingToolWindow::class.java.classLoader.getResource(bulbWithTheme)?.toExternalForm() else ""
  if (bulbIcon == null) LOG.warn("Cannot find bulb icon")

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createHintBlockTemplate(hintElement: Element, displayedHintNumber: String, escapedHintTitle: String): String {
    val iconSize = getHintIconSize()
    return """
      <img src='$bulbIcon' width='$iconSize' height='$iconSize' >
      <span><a href='$HINT_PROTOCOL$displayedHintNumber' value='${hintElement.text()}'>$escapedHintTitle $displayedHintNumber</a>
      <span id='chevron'>$CHEVRON_RIGHT</span></span>
    """.trimIndent()
  }

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createExpandedHintBlockTemplate(hintElement: Element, displayedHintNumber: String, escapedHintTitle: String): String {
    val hintText = hintElement.text()
    val iconSize = getHintIconSize()
    return """ 
        <img src='$bulbIcon' width='$iconSize' height='$iconSize' >
        <span><a href='$HINT_PROTOCOL$displayedHintNumber' value='$hintText'>$escapedHintTitle $displayedHintNumber</a>
        <span id='chevron'>$CHEVRON_DOWN</span></span>
        <div class='hint_text'>$hintText</div>
     """.trimIndent()
  }

  if (displayedHintNumber.isEmpty() || displayedHintNumber == "1") {
    hintElement.wrap("<div class='top'></div>")
  }
  val course = StudyTaskManager.getInstance(project).course
  val escapedHintTitle = hintTitle.xmlEscaped
  return if (course != null && !course.isStudy) {
    createExpandedHintBlockTemplate(hintElement, displayedHintNumber, escapedHintTitle)
  }
  else {
    createHintBlockTemplate(hintElement, displayedHintNumber, escapedHintTitle)
  }
}

fun JPanel.addBorder(newBorder: Border?): JPanel {
  return apply {
    border = JBUI.Borders.compound(newBorder, border)
  }
}

fun Row.createButton(isCheckbox: Boolean): Cell<JToggleButton> {
  return if (isCheckbox) {
    checkBox("")
  } else {
    radioButton("")
  }
}
