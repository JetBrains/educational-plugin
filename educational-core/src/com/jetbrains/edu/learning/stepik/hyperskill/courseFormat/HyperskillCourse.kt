package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {
  @Suppress("unused") constructor() // used for deserialization

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  var hyperskillProject: HyperskillProject? = null

  constructor(hyperskillProject: HyperskillProject, languageID: String, environment: String) {
    this.hyperskillProject = hyperskillProject
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    language = languageID
    this.environment = environment
  }

  val isTemplateBased: Boolean
    get() {
      return (hyperskillProject ?: error("Disconnected ${EduNames.JBA} project")).isTemplateBased
    }


  fun getProjectLesson(): FrameworkLesson? = lessons.firstOrNull() as? FrameworkLesson

  fun getProblemsLesson(): Lesson? = getLesson(HYPERSKILL_PROBLEMS)

  fun isTaskInProject(task: Task): Boolean = task.lesson == getProjectLesson()

  private fun descriptionNote(projectId: Int): String =
    "<br/><br/>Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org/projects/$projectId</a>"

  override fun getItemType(): String = HYPERSKILL

  override fun getId(): Int {
    return hyperskillProject?.id ?: super.getId()
  }

  fun findOrCreateProblemsLesson(): Lesson {
    var lesson = getProblemsLesson()
    if (lesson == null) {
      lesson = Lesson().apply {
        name = HYPERSKILL_PROBLEMS
        index = this@HyperskillCourse.items.size + 1
        course = this@HyperskillCourse
      }
      addLesson(lesson)
    }
    return lesson
  }
}
