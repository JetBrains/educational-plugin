package com.jetbrains.edu.learning.courseFormat.tasks

class ChoiceTask : Task {

  var choiceOptions = listOf<ChoiceOption>()
  var isMultipleChoice: Boolean = false
  var selectedVariants = mutableListOf<Int>()

  //used for deserialization
  @Suppress("unused")
  constructor()

  constructor(name: String) : super(name)

  override fun getItemType(): String = "choice"

  class ChoiceOption {
    var text: String = ""
    var status: OptionStatus = OptionStatus.UNKNOWN

    //used for deserialization
    @Suppress("unused")
    private constructor()

    constructor(text: String) {
      this.text = text
    }

    constructor(text: String, status: OptionStatus) {
      this.text = text
      this.status = status
    }
  }

  /**
   * Choice tasks created on Stepik can't be checked locally, so they have UNKNOWN status
   */
  enum class OptionStatus {
    CORRECT, INCORRECT, UNKNOWN
  }
}
