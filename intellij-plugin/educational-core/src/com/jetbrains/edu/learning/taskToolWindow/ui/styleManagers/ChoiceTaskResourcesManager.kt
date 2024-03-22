package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.google.gson.Gson
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskToolWindow.ui.getRadioButtonCSS
import kotlinx.css.*
import kotlinx.css.properties.lh

class ChoiceTaskResourcesManager : TaskResourcesManager<ChoiceTask> {

  // because task object is inserted after html is loaded
  private fun getTaskResources(task: ChoiceTask): Map<String, Any> = mapOf(
    "text" to task.presentableQuizHeader,
    "selected_variants" to task.selectedVariants,
    "choice_options" to Gson().toJson(task.choiceOptions.map { it.text }),
    "is_multiple_choice" to task.isMultipleChoice,
    "is_disabled" to (task.status == CheckStatus.Failed && task.isChangedOnFailed),
    "choice_options_style" to choiceOptionsStylesheet()
  )

  override fun getText(task: ChoiceTask): String {
    return if (task.choiceOptions.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(CHOICE_TASK_TEMPLATE, getTaskResources(task))
    }
    else {
      EduCoreBundle.message("ui.task.specific.panel", stepLink(task.id), EduNames.JBA)
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
    }.toString()
      .plus(getRadioButtonCSS())
  }

  companion object {
    private const val CHOICE_TASK_TEMPLATE = "choiceTask.html"
  }
}