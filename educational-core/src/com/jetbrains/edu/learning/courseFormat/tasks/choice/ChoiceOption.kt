package com.jetbrains.edu.learning.courseFormat.tasks.choice

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