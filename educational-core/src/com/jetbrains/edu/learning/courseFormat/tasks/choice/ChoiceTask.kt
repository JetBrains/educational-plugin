package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class ChoiceTask : Task {

  var choiceOptions = listOf<ChoiceOption>()
  var isMultipleChoice: Boolean = false
  var selectedVariants = mutableListOf<Int>()

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

  constructor(name: String) : super(name)

  override fun getItemType(): String = "choice"
}
