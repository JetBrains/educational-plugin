package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {
  @Suppress("unused") constructor() // used for deserialization

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  lateinit var hyperskillProject: HyperskillProject

  constructor(hyperskillProject: HyperskillProject, languageID: String) {
    this.hyperskillProject = hyperskillProject
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    language = languageID
  }

  fun getProjectLesson(): FrameworkLesson? = lessons.firstOrNull() as? FrameworkLesson

  fun isTaskInProject(task: Task): Boolean = task.lesson == getProjectLesson()

  private fun descriptionNote(projectId: Int): String =
    "<br/><br/>Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org/projects/$projectId</a>"

  override fun getItemType(): String = HYPERSKILL
}
