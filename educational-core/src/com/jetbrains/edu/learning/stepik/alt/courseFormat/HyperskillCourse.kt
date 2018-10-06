package com.jetbrains.edu.learning.stepik.alt.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Tag

class HyperskillCourse : Course {
  @Suppress("unused") constructor() // used for deserialization

  constructor(name: String, languageID: String) {
    setName(name)
    description = COURSE_DESCRIPTION
    language = languageID
  }

  override fun getTags(): List<Tag> {
    val tags = super.getTags()
    tags.add(Tag(EduNames.ADAPTIVE))
    return tags
  }


  companion object {
    private const val COURSE_DESCRIPTION = "This is a Hyperskill course.<br/><br/>" +
                                           "Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org</a>"
  }
}
