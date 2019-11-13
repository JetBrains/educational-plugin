package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.util.*

class ChoiceTask : Task {

  var choiceOptions = listOf<ChoiceOption>()
  var isMultipleChoice: Boolean = false
  var selectedVariants = mutableListOf<Int>()
  var messageCorrect: String = CheckUtils.CONGRATULATIONS
  var messageIncorrect: String = "Incorrect solution"

  val canCheckLocally: Boolean
    get() {
      if (choiceOptions.any { it.status == ChoiceOptionStatus.UNKNOWN }) {
        return false
      }
      return !(course is EduCourse && (course as EduCourse).isRemote && course.isStudy)
    }

  //used for deserialization
  @Suppress("unused")
  constructor()

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override fun getItemType(): String = "choice"

  override fun supportSubmissions(): Boolean = true

  override fun isPluginTaskType() = false
}
