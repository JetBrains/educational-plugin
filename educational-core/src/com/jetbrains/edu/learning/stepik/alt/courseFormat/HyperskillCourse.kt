package com.jetbrains.edu.learning.stepik.alt.courseFormat

import com.jetbrains.edu.learning.courseFormat.Course

class HyperskillCourse : Course {
  @Suppress("unused") constructor() // used for deserialization

  constructor(name: String, languageID: String) {
    setName(name)
    description = COURSE_DESCRIPTION
    language = languageID
  }

  companion object {
    private const val COURSE_DESCRIPTION = "This is a Hyperskill course.<br/><br/>" +
                                           "Learn everything in Java<br/><br/>" +
                                           "Learn more <a href=\"https://hyperskill.org\">https://hyperskill.org</a>"
  }
}
