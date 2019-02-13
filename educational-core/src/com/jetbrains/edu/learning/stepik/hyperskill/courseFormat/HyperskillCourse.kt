package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillTopic
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {
  @Suppress("unused") constructor() // used for deserialization

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  var hyperskillId: Int = 0

  constructor(hyperskillProject: HyperskillProject, languageID: String) {
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    hyperskillId = hyperskillProject.id
    language = languageID
    courseType = HYPERSKILL
  }

  // temporary solution for hyperskill java courses
  // TODO: store language version on server side
  override fun getLanguageVersion(): String = "11"

  private fun descriptionNote(projectId: Int): String = "<br/><br/>Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org/projects/${projectId}</a>"
}
