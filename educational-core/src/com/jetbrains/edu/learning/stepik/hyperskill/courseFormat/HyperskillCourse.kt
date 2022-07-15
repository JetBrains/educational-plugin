package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder.StepikTaskType
import com.jetbrains.edu.learning.stepik.hyperskill.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {

  constructor()

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  var hyperskillProject: HyperskillProject? = null
    set(value) {
      field = value
      id = value?.id ?: 0
    }

  var selectedStage: Int? = null
  var selectedProblem: Int? = null

  constructor(hyperskillProject: HyperskillProject, languageID: String, environment: String) {
    this.hyperskillProject = hyperskillProject
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    programmingLanguage = languageID
    this.environment = environment
    id = hyperskillProject.id
  }

  constructor(languageName: String, languageID: String) {
    name = getProblemsProjectName(languageName)
    description = EduCoreBundle.message("hyperskill.problems.project.description", languageName.capitalize())
    programmingLanguage = languageID
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

  fun isTaskInTopicsSection(task: Task): Boolean = getTopicsSection()?.lessons?.contains(task.lesson) == true

  private fun descriptionNote(projectId: Int): String {
    val link = "$HYPERSKILL_PROJECTS_URL/$projectId"
    return """<br/><br/>${EduCoreBundle.message("learn.more.at")} <a href="${wrapWithUtm(link, "project-card")}">$link</a>"""
  }

  override val itemType: String = HYPERSKILL

  // lexicographical order
  companion object {
    private val SUPPORTED_STEP_TYPES: Set<String> = setOf(
      StepikTaskType.CHOICE.type,
      StepikTaskType.CODE.type,
      StepikTaskType.DATASET.type,
      StepikTaskType.NUMBER.type,
      StepikTaskType.PYCHARM.type,
      StepikTaskType.STRING.type,
      StepikTaskType.TEXT.type
    )

    fun isStepSupported(type: String?): Boolean = type in SUPPORTED_STEP_TYPES
  }
}
