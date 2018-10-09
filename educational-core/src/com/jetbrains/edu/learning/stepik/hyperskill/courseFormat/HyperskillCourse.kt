package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.courseFormat.Course

class HyperskillCourse : Course {
  private var _id: Int = -1

  override fun getId(): Int = _id

  fun setId(id: Int) {
    _id = id
  }

  @Suppress("unused") constructor() // used for deserialization

  constructor(name: String, languageID: String) {
    setName(name)
    description = COURSE_DESCRIPTION
    language = languageID
  }

  companion object {
    private const val COURSE_DESCRIPTION = "This is a Hyperskill course.<br/><br/>" +
                                           "Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org</a>"
  }
}
