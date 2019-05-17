package com.jetbrains.edu.learning.courseFormat.tasks

class ChoiceTask : Task {

  var choiceVariants = mutableListOf<String>()
  var isMultipleChoice: Boolean = false
  var selectedVariants = mutableListOf<Int>()

  //used for deserialization
  constructor()

  constructor(name: String) : super(name)

  override fun getItemType(): String = "choice"
}
