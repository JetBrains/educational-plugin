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

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ChoiceOption

    if (text != other.text) return false
    if (status != other.status) return false

    return true
  }

  override fun hashCode(): Int {
    var result = text.hashCode()
    result = 31 * result + status.hashCode()
    return result
  }
}