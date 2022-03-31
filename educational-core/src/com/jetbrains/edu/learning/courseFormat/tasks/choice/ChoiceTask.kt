package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import java.util.*

class ChoiceTask : Task {

  var choiceOptions: List<ChoiceOption> = listOf()
  var isMultipleChoice: Boolean = false
  var selectedVariants: MutableList<Int> = mutableListOf()
  var messageCorrect: String = EduCoreBundle.message("check.correct.solution")
  var messageIncorrect: String = EduCoreBundle.message("check.incorrect.solution")
  var quizHeader: String =
    if (isMultipleChoice) EduCoreBundle.message("course.creator.create.choice.task.multiple.label")
    else EduCoreBundle.message("course.creator.create.choice.task.single.label")

  val canCheckLocally: Boolean
    get() {
      if (choiceOptions.any { it.status == ChoiceOptionStatus.UNKNOWN }) {
        return false
      }
      if (course is HyperskillCourse) return false
      return !(course is EduCourse && (course as EduCourse).isStepikRemote && course.isStudy)
    }

  //used for deserialization
  constructor()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = CHOICE_TASK_TYPE

  override fun isPluginTaskType() = false

  override fun isChangedOnFailed(): Boolean = !canCheckLocally

  //Is called from choiceTask.html.ft
  @Suppress("unused")
  fun addSelectedVariant(variant: Int) {
    selectedVariants.add(variant)
  }

  //Is called from choiceTask.html.ft
  @Suppress("unused")
  fun removeSelectedVariant(variant: Int) {
    selectedVariants.remove(variant)
  }

  //Is called from choiceTask.html.ft
  @Suppress("unused")
  fun clearSelectedVariants() {
    selectedVariants.clear()
  }

  companion object {
    const val CHOICE_TASK_TYPE: String = "choice"
  }
}
