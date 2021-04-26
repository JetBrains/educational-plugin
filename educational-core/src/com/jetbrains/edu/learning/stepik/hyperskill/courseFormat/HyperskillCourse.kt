package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder.StepikTaskType
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROBLEMS
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TOPICS
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.getProblemsProjectName
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

  constructor(languageName: String, languageID: String) {
    name = getProblemsProjectName(languageName)
    description = EduCoreBundle.message("hyperskill.problems.project.description", languageName.capitalize())
    language = languageID
  }

  val isTemplateBased: Boolean
    get() {
      return (hyperskillProject ?: error("Disconnected ${EduNames.JBA} project")).isTemplateBased
    }


  fun getProjectLesson(): FrameworkLesson? = lessons.firstOrNull() as? FrameworkLesson

  /**
   * Deprecated. Hyperskill problems are used to be stored in [HYPERSKILL_PROBLEMS] lesson.
   *
   * Structure example:
   *
   * [HYPERSKILL_PROBLEMS] lesson
   *
   *     `Arithmetic average` task
   *
   *     `Thread-safe account` task
   *
   *     `Countdown counter` task
   *
   *    etc
   *
   */
  @Deprecated("Problems lesson isn't used anymore, use Topics section instead", replaceWith = ReplaceWith("getTopicsSection()"))
  fun getProblemsLesson(): Lesson? = getLesson { it.presentableName == HYPERSKILL_PROBLEMS }

  /**
   * Hyperskill problems are grouped by their topics. Topics are lessons located in [HYPERSKILL_TOPICS] section.
   *
   * Structure example:
   *
   * [HYPERSKILL_TOPICS] section
   *
   *    `The for-loop` lesson
   *
   *        `Arithmetic average` task
   *
   *        `Size of parts` task
   *
   *        etc
   *
   *     `Thread synchronization` lesson
   *
   *        `Thread-safe account` task
   *
   *        `Countdown counter` task
   *
   *        etc
   *
   */
  fun getTopicsSection(): Section? = getSection { it.presentableName == HYPERSKILL_TOPICS }

  fun getProblem(id: Int): Task? {
    getTopicsSection()?.lessons?.forEach { lesson ->
      lesson.getTask(id)?.let {
        return it
      }
    }
    return null
  }

  fun isTaskInProject(task: Task): Boolean = task.lesson == getProjectLesson()

  private fun descriptionNote(projectId: Int): String =
    "<br/><br/>Learn more at <a href=\"https://hyperskill.org\">https://hyperskill.org/projects/$projectId</a>"

  override fun getItemType(): String = HYPERSKILL

  override fun getId(): Int {
    return hyperskillProject?.id ?: super.getId()
  }

  override fun isViewAsEducatorEnabled(): Boolean = false

  companion object {
    val SUPPORTED_STEP_TYPES: Set<String> = setOf(StepikTaskType.CODE.type, StepikTaskType.TEXT.type)
  }
}
