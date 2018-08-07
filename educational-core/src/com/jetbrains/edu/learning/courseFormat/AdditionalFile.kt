package com.jetbrains.edu.learning.courseFormat

class AdditionalFile() : StudyFile() {

  constructor(text: String, isVisible: Boolean) : this() {
    this.text = text
    this.isVisible = isVisible
  }
}
