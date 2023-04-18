package com.jetbrains.edu.learning.taskDescription.ui.styleManagers.sortingBasedTask

import com.google.gson.Gson
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskDescription.isNewUI
import com.jetbrains.edu.learning.taskDescription.ui.MatchingTaskUI
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TaskResourcesManager
import kotlinx.css.*
import kotlinx.css.properties.LineHeight
import kotlinx.css.properties.Timing
import kotlinx.css.properties.s
import kotlinx.css.properties.transition

abstract class SortingBasedTaskResourcesManager<T : SortingBasedTask> : TaskResourcesManager<T> {
  protected abstract val templateName: String

  protected open fun getTextResources(task: T): Map<String, String> {
    val moveUpUrl = StyleResourcesManager.resourceUrl(getIconPath(isDown = false))
    val moveDownUrl = StyleResourcesManager.resourceUrl(getIconPath(isDown = true))
    return mapOf(
      "options" to Gson().toJson(task.options),
      "ordering" to Gson().toJson(task.ordering.toList()),
      "upButtonIconPath" to moveUpUrl,
      "downButtonIconPath" to moveDownUrl,
    )
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
        label {
          height = 20.px
          fontFamily = styleManager.codeFont
          fontStyle = FontStyle.normal
          fontWeight = FontWeight.lighter
          fontSize = 13.px
          lineHeight = LineHeight(20.px.value)
          color = MatchingTaskUI.Value.foreground().asCssColor()
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
      }.toString()
    }

  private fun getIconPath(isDown: Boolean): String {
    var index = 0

    if (isNewUI()) index += 4

    if (isDown) index += 2

    if (UIUtil.isUnderDarcula()) index += 1

    return StyleResourcesManager.sortingBasedTaskResourcesList[index]
  }
}
