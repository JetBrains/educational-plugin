package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.sortingBasedTask

import com.google.gson.Gson
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskToolWindow.isNewUI
import com.jetbrains.edu.learning.taskToolWindow.ui.MatchingTaskUI
import com.jetbrains.edu.learning.taskToolWindow.ui.getSortingShortcutHTML
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TaskResourcesManager
import kotlinx.css.*
import kotlinx.css.Float
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.Timing
import kotlinx.css.properties.s
import kotlinx.css.properties.transition

abstract class SortingBasedTaskResourcesManager<T : SortingBasedTask> : TaskResourcesManager<T> {
  private val templateName: String = "matchingTask.html"

  protected abstract fun getCaptions(task: T): String

  protected abstract fun taskSpecificStyles(): Map<String, String>

  private fun getTextResources(task: T): Map<String, String> {
    val moveUpUrl = StyleResourcesManager.resourceUrl(getIconPath(isDown = false))
    val moveDownUrl = StyleResourcesManager.resourceUrl(getIconPath(isDown = true))
    val styleName = wrapIntoStyleName(task.itemType)
    return mapOf(
      "sorting_based_style" to "\${$styleName}",
      "options" to Gson().toJson(task.options),
      "ordering" to Gson().toJson(task.ordering.toList()),
      "upButtonIconPath" to moveUpUrl,
      "downButtonIconPath" to moveDownUrl,
      "tutorial" to getTutorialHTML(moveUpUrl, moveDownUrl),
      "captions" to getCaptions(task),
    )
      .plus(taskSpecificStyles())
  }

  protected fun wrapIntoStyleName(s: String) = "${s}_style"

  private fun getTutorialHTML(moveUpUrl: String, moveDownUrl: String): String {
    val xIcon = "<img src='${moveUpUrl}' class='imgShortcut'>"
    val yIcon = "<img src='${moveDownUrl}' class='imgShortcut'>"

    return getSortingShortcutHTML(xIcon, yIcon)
  }

  override fun getText(task: T): String {
    return if (task.options.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(templateName, getTextResources(task))
    }
    else {
      EduCoreBundle.message("ui.task.specific.panel", stepLink(task.id), EduNames.JBA)
    }
  }

  protected open val stylesheet: String
    get() {
      val styleManager = StyleManager()
      return CSSBuilder().apply {
        "#keyValueGrid" {
          display = Display.grid
          rowGap = RowGap(12.px.value)
          alignItems = Align.stretch
          justifyContent = JustifyContent.stretch
        }
        ".value" {
          display = Display.flex
          flexDirection = FlexDirection.row
          alignItems = Align.center
          justifyContent = JustifyContent.stretch
          padding = "10px 8px 10px 12px"
          gap = Gap(8.px.value)
          background = MatchingTaskUI.Value.background().asCssColor().value
          border = "1px solid"
          borderColor = MatchingTaskUI.Value.borderColor().asCssColor()
          borderRadius = 4.px
        }
        ".value:focus" {
          val focusedBorderColor = UIUtil.getFocusedBorderColor().asCssColor().value
          put("outline", "1px solid $focusedBorderColor")
          border = "1px solid $focusedBorderColor"
        }
        label {
          fontFamily = styleManager.bodyFont
          fontStyle = FontStyle.normal
          fontWeight = FontWeight.lighter
          fontSize = 13.px
          lineHeight = LineHeight(20.px.value)
          color = MatchingTaskUI.Value.foreground().asCssColor()
        }
        code {
          fontFamily = styleManager.codeFont
        }
        ".labelPanel" {
          padding = "0"
        }
        ".buttonPanel" {
          display = Display.flex
          flexDirection = FlexDirection.column
          alignItems = Align.flexStart
          justifyContent = JustifyContent.flexEnd
          marginLeft = LinearDimension.auto
          flexShrink = 0.0
          padding = "0"
          gap = Gap(4.px.value)
        }
        button {
          width = 16.px
          height = 16.px
          padding = "0"
          border = "0"
          overflow = Overflow.visible
          backgroundColor = Color.transparent
          transition("color", .15.s, Timing.easeInOut)
          transition("background-color", .15.s, Timing.easeInOut)
          transition("border-color", .15.s, Timing.easeInOut)
          transition("box-shadow", .15.s, Timing.easeInOut)
        }
        "button:not(:disabled)" {
          cursor = Cursor.pointer
        }
        "button:disabled" {
          cursor = Cursor.notAllowed
        }
        "#shortcutLabel" {
          padding = "4px 0"
          marginBottom = 8.px
        }
        "#shortcutLabel > label" {
          fontSize = 13.px
          verticalAlign = VerticalAlign.middle
          lineHeight = LineHeight(16.px.value)
          color = JBUI.CurrentTheme.ContextHelp.FOREGROUND.asCssColor()
        }
        ".shortcut-description" {
          display = Display.inlineBlock
          color = MatchingTaskUI.Key.foreground().asCssColor()
          backgroundColor = MatchingTaskUI.Key.background().asCssColor()
          borderRadius = 4.px
          verticalAlign = VerticalAlign.middle
        }
        ".imgShortcut" {
          padding = "2px"
          height = 16.px
          width = 16.px
          float = Float.left
          verticalAlign = VerticalAlign.middle
        }
        ".textShortcut" {
          padding = "2px 4px"
        }
      }.toString()
    }

  private fun getIconPath(isDown: Boolean): String {
    var index = 0

    if (isNewUI()) index += 4

    if (isDown) index += 2

    if (!JBColor.isBright()) index += 1

    return StyleResourcesManager.sortingBasedTaskResourcesList[index]
  }
}
