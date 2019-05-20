package com.jetbrains.edu.learning.courseFormat.tasks.choice

class ChoiceOption {
  var text: String = ""
  var status: ChoiceOptionStatus = ChoiceOptionStatus.UNKNOWN

  //used for deserialization
  @Suppress("unused")
  private constructor()

  constructor(text: String) {
    this.text = text
  }

  constructor(text: String, status: ChoiceOptionStatus) {
    this.text = text
    this.status = status
  }
}