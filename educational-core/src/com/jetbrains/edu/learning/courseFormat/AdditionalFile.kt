package com.jetbrains.edu.learning.courseFormat

class AdditionalFile() : StudyFile() {

  constructor(text: String, isVisible: Boolean) : this() {
    this.setText(text)
    this.isVisible = isVisible
  }
}
