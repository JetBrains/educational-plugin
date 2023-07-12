package com.jetbrains.edu.learning.checkio.courseFormat

import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.annotations.NonNls

class CheckiOCourse : Course {
  // used for deserialization
  constructor()
  constructor(name: String, languageId: String) {
    super.name = name
    description = COURSE_DESCRIPTION
    this.languageId = languageId
  }

  fun addStation(station: CheckiOStation) {
    addLesson(station)
  }

  val stations: List<CheckiOStation>
    get() = items.filterIsInstance(CheckiOStation::class.java)

  override fun toString(): String =
    "stations=[${stations.joinToString("\n") { obj: CheckiOStation -> obj.toString() }}]"

  override val itemType: String
    get() = CheckiONames.CHECKIO

  companion object {
    private val COURSE_DESCRIPTION: @NonNls String = """
         CheckiO is a game where you code in Python or JavaScript.
         Progress in the game by solving code challenges and compete for the most elegant and creative solutions.
         <a href="http://www.checkio.org/">http://www.checkio.org/</a>
         """.trimIndent()
  }
}
