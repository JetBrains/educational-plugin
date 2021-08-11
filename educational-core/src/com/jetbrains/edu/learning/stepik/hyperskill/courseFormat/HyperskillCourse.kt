package com.jetbrains.edu.learning.stepik.hyperskill.courseFormat

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder.StepikTaskType
import com.jetbrains.edu.learning.stepik.hyperskill.*
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

  fun isTaskInTopicsSection(task: Task): Boolean = getTopicsSection()?.lessons?.contains(task.lesson) == true

  private fun descriptionNote(projectId: Int): String {
    val link = "https://hyperskill.org/projects/$projectId"
    return """<br/><br/>Learn more at <a href="${wrapWithUtm(link, "project-card")}">$link</a>"""
  }

  override fun getItemType(): String = HYPERSKILL

  override fun getId(): Int {
    return hyperskillProject?.id ?: super.getId()
  }

  override fun isViewAsEducatorEnabled(): Boolean = false

  companion object {
    val SUPPORTED_STEP_TYPES: Set<String> = setOf(
      StepikTaskType.CODE.type,
      StepikTaskType.TEXT.type,
      StepikTaskType.PYCHARM.type,
      StepikTaskType.DATASET.type
    )

    private val REMOTELY_CHECKED_TASKS: Set<Class<out Task>> = setOf(CodeTask::class.java, DataTask::class.java)

    fun Task.isRemotelyChecked(): Boolean = javaClass in REMOTELY_CHECKED_TASKS
  }
}
