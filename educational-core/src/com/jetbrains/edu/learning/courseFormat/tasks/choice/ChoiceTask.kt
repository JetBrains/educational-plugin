package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.message
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*

class ChoiceTask : Task {

  var choiceOptions: List<ChoiceOption> = listOf()
  var isMultipleChoice: Boolean = false
  var selectedVariants: MutableList<Int> = mutableListOf()
  var messageCorrect: String = message("check.correct.solution")
  var messageIncorrect: String = message("check.incorrect.solution")
  var quizHeader: String =
    if (isMultipleChoice) message("course.creator.create.choice.task.multiple.label")
    else message("course.creator.create.choice.task.single.label")

  var canCheckLocally: Boolean = true

  //used for deserialization
  constructor()

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = CHOICE_TASK_TYPE

  override val isPluginTaskType: Boolean
    get() = false

  override val isChangedOnFailed: Boolean
    get() = !canCheckLocally

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
