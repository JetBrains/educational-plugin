package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.github.PostToGithubActionProvider
import com.jetbrains.edu.learning.marketplace.MarketplaceNotificationUtils
import com.jetbrains.edu.learning.marketplace.isMarketplaceStudentCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls

class PostMarketplaceProjectToGitHub : DumbAwareAction() {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    e.presentation.isEnabledAndVisible = project.isMarketplaceStudentCourse()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project.takeIf { it != null && !it.isDisposed } ?: return
    val postToGithubActionProvider = PostToGithubActionProvider.first() ?: error("PostToGithubActionProvider not found")
    val course = project.course as? EduCourse ?: error("Marketplace course is expected")
    val courseDir = project.courseDir

    val frameworkLessons = course.frameworkLessons()
    if (frameworkLessons.isEmpty()) {
      LOG.info("No framework lessons found for course ${course.id}")
      return showFrameworkLessonsNotFoundNotification(project)
    }
    for (lesson in frameworkLessons) {
      val taskList = lesson.taskList
      val currentTask = lesson.currentTask() ?: taskList.first()
      val taskToNavigate = taskList.lastOrNull { it !is TheoryTask && it.isSolved } ?: currentTask
      NavigationUtils.navigateToTask(project, taskToNavigate, currentTask, showDialogIfConflict = false)
    }
    val excludedLessons = course.lessons - frameworkLessons

    generateGitignore(frameworkLessons, excludedLessons, course, courseDir)
    generateReadme(course, courseDir)

    postToGithubActionProvider.postToGitHub(project, project.courseDir)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  /**
   * To change the content of the generated `.gitignore` file, please change the content of `github_marketplace.gitignore.ft` file
   *
   * @see fileTemplates.internal
   */
  private fun generateGitignore(
    frameworkLessons: Set<FrameworkLesson>,
    excludedLessons: List<Lesson>,
    course: EduCourse,
    courseDir: VirtualFile
  ) {
    val excludeLessonsString = excludedLessons.map { it.name }.joinToString("\n") { "$it/" }
    val frameworkLessonsString = frameworkLessons.map { it.name }.joinToString("\n") { "$it/*/\n$it/$TASK/*/" }
    val solutionDirsString = frameworkLessons.map { it.name }.joinToString("\n") { "!/${it}/$TASK/\n!/${it}/$TASK/$SRC/" }

    val templateVariables = mapOf(
      EXCLUDED_LESSONS to excludeLessonsString,
      INCLUDED_LESSONS to frameworkLessonsString,
      SOLUTION_DIRS to solutionDirsString
    )

    GeneratorUtils.createFileFromTemplate(
      CourseInfoHolder.fromCourse(course, courseDir),
      courseDir,
      GITIGNORE_FILE_PATH,
      GITIGNORE_TEMPLATE_NAME,
      templateVariables
    )
  }

  /**
   * To change the content of the generated `README.md` file, please change the content of `github_marketplace.readme.ft` file
   *
   * @see fileTemplates.internal
   */
  private fun generateReadme(course: EduCourse, courseDir: VirtualFile) {
    val templateVariables = mapOf(
      COURSE_NAME to course.presentableName
    )

    GeneratorUtils.createFileFromTemplate(
      CourseInfoHolder.fromCourse(course, courseDir),
      courseDir,
      README_FILE_PATH,
      README_TEMPLATE_NAME,
      templateVariables
    )
  }

  private fun showFrameworkLessonsNotFoundNotification(project: Project) = Notification(
    MarketplaceNotificationUtils.JETBRAINS_ACADEMY_GROUP_ID,
    EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.notification.title.failed.to.post.project.to.marketplace"),
    EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.notification.content.no.framework.lessons.found"),
    NotificationType.ERROR
  ).notify(project)

  companion object {
    private val LOG: Logger = logger<PostMarketplaceProjectToGitHub>()

    @NonNls
    private const val ACTION_ID: String = "Educational.Student.PostMarketplaceProjectToGitHub"

    @NonNls
    private const val COMPLETION_CONDITION_REGISTRY_KEY: String = "edu.marketplace.PostMarketplaceProjectToGitHub.completion.condition"

    @NonNls
    private const val IS_PROMPTED = "com.jetbrains.edu.Marketplace.PostToGithub.Prompt.Count"

    private const val GITIGNORE_FILE_PATH: String = ".gitignore"
    private const val GITIGNORE_TEMPLATE_NAME: String = "github_marketplace.gitignore"
    private const val README_FILE_PATH: String = "README.md"
    private const val README_TEMPLATE_NAME: String = "github_marketplace.readme"

    /**
     * Template variables for `github_marketplace.gitignore.ft` and `github_marketplace.readme.ft`
     * @see fileTemplates.internal
     */
    private const val EXCLUDED_LESSONS: String = "excludedLessons"
    private const val COURSE_NAME: String = "courseName"
    private const val INCLUDED_LESSONS: String = "includedLessons"
    private const val SOLUTION_DIRS: String = "solutionDirs"

    private const val SRC: String = "src"
    private const val TASK: String = "task"

    private val COMPLETION_CONDITION: Int
      get() = Registry.intValue(COMPLETION_CONDITION_REGISTRY_KEY).coerceIn(0, 100)

    fun promptIfNeeded(project: Project) {
      val course = project.course as? EduCourse ?: return
      if (!isPrompted() && canBePrompted(course)) {
        val inlineBanner = createInlineBanner()
        TaskToolWindowView.getInstance(project).addInlineBanner(inlineBanner)
      }
    }

    private fun EduCourse.frameworkLessons(): Set<FrameworkLesson> {
      return lessons.filterIsInstance<FrameworkLesson>().toSet()
    }

    /**
     * Since the action is only available for courses with FrameworkLessons, we do not prompt it for courses devoid of such lessons
     */
    private fun canBePrompted(course: EduCourse): Boolean {
      val tasks = course.frameworkLessons().flatMap { it.taskList }.filter { it !is TheoryTask }
      if (tasks.isEmpty()) {
        return false
      }
      val numberOfSolvedTasks = tasks.count { it.isSolved }
      return 100 * numberOfSolvedTasks.toDouble() / tasks.size >= COMPLETION_CONDITION
    }

    private fun isPrompted(): Boolean {
      return PropertiesComponent.getInstance().getBoolean(IS_PROMPTED, false)
    }

    private fun setPrompted() {
      PropertiesComponent.getInstance().setValue(IS_PROMPTED, true)
    }

    private fun createInlineBanner(): InlineBanner = InlineBanner(EditorNotificationPanel.Status.Info).apply {
      setMessage(EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.banner.message"))
      addAction(EduCoreBundle.message("action.Educational.Student.PostMarketplaceProjectToGitHub.banner.action")) {
        val action = ActionManager.getInstance().getAction(ACTION_ID)
        ActionManager.getInstance().tryToExecute(action, null, null, null, false)
        setPrompted()
        removeFromParent()
      }
    }
  }
}
