package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.google.gson.Gson
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import kotlinx.css.*
import kotlinx.css.properties.*

class ChoiceTaskResourcesManager : TaskResourcesManager<ChoiceTask> {
  override val resources: Map<String, String>
    get() = mapOf("choice_options_style" to choiceOptionsStylesheet())

  // because task object is inserted after html is loaded
  private fun getTaskResources(task: ChoiceTask): Map<String, Any> = mapOf(
    "text" to task.presentableQuizHeader,
    "selected_variants" to task.selectedVariants,
    "choice_options" to Gson().toJson(task.choiceOptions.map { it.text }),
    "is_multiple_choice" to task.isMultipleChoice,
    "is_disabled" to (task.status == CheckStatus.Failed && task.isChangedOnFailed)
  )

  override fun getText(task: ChoiceTask): String {
    return if (task.choiceOptions.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(CHOICE_TASK_TEMPLATE, getTaskResources(task))
    }
    else {
      when (task.course) {
        is HyperskillCourse -> EduCoreBundle.message("ui.task.specific.panel", stepLink(task.id), EduNames.JBA)
        else -> EduCoreBundle.message("ui.task.specific.panel", getStepikLink(task, task.lesson), StepikNames.STEPIK)
      }
    }
  }

  private fun choiceOptionsStylesheet(): String {
    val styleManager = StyleManager()
    return CSSBuilder().apply {
      "code" {
        fontFamily = styleManager.codeFont
        backgroundColor = styleManager.codeBackground
        padding = "4 4 4 4"
        borderRadius = 5.px
      }
      "#choiceOptions" {
        fontFamily = styleManager.bodyFont
        fontSize = if (isJCEF()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
        lineHeight = (1.3).em.lh
        color = styleManager.bodyColor
        textAlign = TextAlign.left
        paddingLeft = 8.px
      }
      ".optionsBox" {
        display = Display.grid
        gridTemplateColumns = GridTemplateColumns(GridAutoRows.minContent, GridAutoRows.auto)
        justifyContent = JustifyContent.stretch
        alignItems = Align.stretch
        columnGap = ColumnGap(0.px.value)
        rowGap = RowGap(5.px.value)
      }
      ".buttonBox" {
        display = Display.flex
        gridColumn = GridColumn("1")
        verticalAlign = VerticalAlign.middle
        alignItems = Align.center
      }
      ".labelBox" {
        gridColumn = GridColumn("2")
        verticalAlign = VerticalAlign.middle
      }
      "#choiceOptions .text" {
        padding = "8px"
      }
      "#choiceOptions .checkbox, .radio" {
        marginTop = 2.px
        marginRight = 9.px
        verticalAlign = VerticalAlign("middle")
        position = Position.relative
        backgroundColor = getBackgroundColor()
        borderWidth = 0.7.px
        borderColor = getBorderColor()
        borderStyle = BorderStyle.solid
        outline = Outline.none
      }
      "#choiceOptions .checkbox" {
        borderRadius = 3.px
        //sets size of the element
        padding = "8px"
      }
      "#choiceOptions .radio" {
        borderRadius = 50.pct
        width = 16.px
        height = 16.px
      }
      "#choiceOptions .radio:checked" {
        padding = "3.2px"
        color = styleManager.bodyColor
        backgroundColor = getRadioButtonCheckedBackgroundColor()
        borderColor = getRadioButtonCheckedBorderColor()
        borderWidth = (5.9).px
      }
      "#choiceOptions .checkbox:checked" {
        backgroundColor = getRadioButtonCheckedBorderColor()
        borderColor = getRadioButtonCheckedBorderColor()
      }
      "#choiceOptions .checkbox:checked:after" {
        display = Display.block
      }
      "#choiceOptions .checkbox:after" {
        display = Display.none
        position = Position.absolute
        content = QuotedString("")
        left = 6.px
        top = 2.px
        width = 3.px
        height = 8.px
        backgroundColor = getRadioButtonCheckedBorderColor()
        border = "solid"
        borderColor = getRadioButtonCheckedBackgroundColor()
        borderTopWidth = 0.px
        borderBottomWidth = 2.px
        borderLeftWidth = 0.px
        borderRightWidth = 2.px
        transform.rotate(35.deg)
      }
      "#choiceOptions .radio:focus, .radio:before, .radio:hover, .checkbox:focus, .checkbox:before, .checkbox:hover" {
        boxShadow += BoxShadow(false, 0.px, 0.px, 2.px, 2.px, getFocusColor())
      }
      "#choiceOptions .disable:focus, .disable:before, .disable:hover" {
        boxShadow = BoxShadows.none
      }
    }.toString()
      .plus("#choiceOptions .checkbox, .radio { -webkit-appearance: none; }")
      .plus(getSystemSpecificCss())
  }

  private fun getBackgroundColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
    }
    else {
      Color(TaskToolWindowBundle.value("choice.options.background.color"))
    }
  }

  private fun getBorderColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
    }
    else {
      Color(TaskToolWindowBundle.value("choice.options.border.color"))
    }
  }

  private fun getRadioButtonCheckedBorderColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
    }
    else {
      Color(TaskToolWindowBundle.value("choice.options.checked.border.color"))
    }
  }

  private fun getRadioButtonCheckedBackgroundColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      Color(TaskToolWindowBundle.value("choice.options.border.color"))
    }
    else {
      Color(TaskToolWindowBundle.value("choice.options.background.color"))
    }
  }

  private fun getFocusColor(): Color {
    return if (UIUtil.isUnderDarcula()) {
      Color(TaskToolWindowBundle.value("darcula.choice.options.focus.color"))
    }
    else {
      Color(TaskToolWindowBundle.value("choice.options.focus.color"))
    }
  }

  private fun getSystemSpecificCss(): String {
    if (SystemInfo.isWindows) {
      return CSSBuilder().apply {
        "#choiceOptions .radio:checked" {
          marginRight = 7.3.px
          left = (-1).px
        }
        "#choiceOptions .checkbox:checked" {
          borderStyle = BorderStyle.none
          padding = "8.7px"
        }
      }.toString()
    }
    return ""
  }

  companion object {
    private const val CHOICE_TASK_TEMPLATE = "choiceTask.html"
  }
}