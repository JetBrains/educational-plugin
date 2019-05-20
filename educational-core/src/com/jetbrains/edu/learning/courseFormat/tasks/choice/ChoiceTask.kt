package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.courseFormat.tasks.Task

class ChoiceTask : Task {

  var choiceOptions = listOf<ChoiceOption>()
  var isMultipleChoice: Boolean = false
  var selectedVariants = mutableListOf<Int>()

  val canCheckLocally get() = choiceOptions.all { it.status != OptionStatus.UNKNOWN }

  //used for deserialization
  @Suppress("unused")
  constructor()

  constructor(name: String) : super(name)

  override fun getItemType(): String = "choice"
}
