package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.actionSystem.ActionPlaces
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checkio.CheckiOPlatformProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraPlatformProvider
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.JetBrainsAcademyPlatformProvider
import com.jetbrains.edu.learning.stepik.newProjectUI.StepikPlatformProvider

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 */
@Suppress("UnstableApiUsage")
class EduCounterUsageCollector : CounterUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  enum class TaskNavigationPlace {
    CHECK_ALL_NOTIFICATION,
    TASK_DESCRIPTION_TOOLBAR,
    CHECK_PANEL,
    UNRESOLVED_DEPENDENCY_NOTIFICATION
  }

  enum class LinkType {
    IN_COURSE, STEPIK, EXTERNAL, PSI, CODEFORCES, JBA
  }

  private enum class AuthorizationEvent {
    LOG_IN, LOG_OUT
  }

  enum class AuthorizationPlace {
    SETTINGS, WIDGET, START_COURSE_DIALOG, SUBMISSIONS_TAB
  }

  private enum class HintEvent {
    EXPANDED, COLLAPSED
  }

  private enum class PostCourseEvent {
    UPLOAD, UPDATE
  }

  enum class SynchronizeCoursePlace {
    WIDGET, PROJECT_GENERATION, PROJECT_REOPEN
  }

  @Suppress("unused") //enum values are not mentioned explicitly
  private enum class CourseSelectionViewSource(private val actionPlace: String? = null) {
    WELCOME_SCREEN(ActionPlaces.WELCOME_SCREEN), MAIN_MENU(ActionPlaces.MAIN_MENU), FIND_ACTION(ActionPlaces.ACTION_SEARCH), UNKNOWN;

    companion object {
      fun fromActionPlace(actionPlace: String): CourseSelectionViewSource {
        return values().firstOrNull { it.actionPlace == actionPlace } ?: UNKNOWN
      }
    }
  }

  @Suppress("unused")
  private enum class CourseSelectionViewTab {
    MARKETPLACE,
    JBA,
    CHECKIO,
    CODEFORCES,
    COURSERA,
    STEPIK,
    MY_COURSES,
    UNKNOWN;

    companion object {
      fun fromProvider(provider: CoursesPlatformProvider): CourseSelectionViewTab {
        return when (provider) {
          is JetBrainsAcademyPlatformProvider -> JBA
          is CheckiOPlatformProvider -> CHECKIO
          is CodeforcesPlatformProvider -> CODEFORCES
          is CourseraPlatformProvider -> COURSERA
          is StepikPlatformProvider -> STEPIK
          is MarketplacePlatformProvider -> MARKETPLACE
          is MyCoursesProvider -> MY_COURSES
          else -> UNKNOWN
        }
      }
    }
  }

  companion object {
    private const val MODE = "mode"
    private const val SOURCE = "source"
    private const val EVENT = "event"
    private const val TYPE = "type"
    private const val LANGUAGE = "language"
    private const val EDU_TAB = "tab"

    private val GROUP = EventLogGroup("educational.counters", 4)

    private val COURSE_MODE_FIELD = EventFields.String(MODE,
                                                       listOf(EduNames.STUDY, CCUtils.COURSE_MODE))
    private val ITEM_TYPE_FIELD = EventFields.String(TYPE, listOf("CheckiO",
                                                                  "PyCharm",
                                                                  "Coursera",
                                                                  "Hyperskill",
                                                                  "Marketplace",
                                                                  "Codeforces",
                                                                  "section",
                                                                  "framework",
                                                                  "lesson",
                                                                  "edu",
                                                                  "ide",
                                                                  "choice",
                                                                  "code",
                                                                  "output",
                                                                  "theory"))
    private val LANGUAGE_FIELD = EventFields.String(LANGUAGE,
                                                    listOf("JAVA", "kotlin", "Python", "Scala", "JavaScript", "Rust", "ObjectiveC", "go"))


    private val TASK_NAVIGATION_EVENT = GROUP.registerEvent("navigate.to.task", enumField<TaskNavigationPlace>(SOURCE))
    private val EDU_PROJECT_CREATED_EVENT = GROUP.registerEvent("edu.project.created", COURSE_MODE_FIELD, ITEM_TYPE_FIELD, LANGUAGE_FIELD)
    private val EDU_PROJECT_OPENED_EVENT = GROUP.registerEvent("edu.project.opened", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)
    private val STUDY_ITEM_CREATED_EVENT = GROUP.registerEvent("study.item.created", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)
    private val LICK_CLICKED_EVENT = GROUP.registerEvent("link.clicked", enumField<LinkType>(TYPE))
    private val AUTHORIZATION_EVENT = GROUP.registerEvent("authorization",
                                                          enumField<AuthorizationEvent>(EVENT),
                                                          EventFields.String("platform", listOf("Hyperskill", "Stepik", "Js_CheckiO", "Py_CheckiO")),
                                                          enumField<AuthorizationPlace>(SOURCE))
    private val SHOW_FULL_OUTPUT_EVENT = GROUP.registerEvent("show.full.output")
    private val PEEK_SOLUTION_EVENT = GROUP.registerEvent("peek.solution")
    private val LEAVE_FEEDBACK_EVENT = GROUP.registerEvent("leave.feedback")
    private val REVERT_TASK_EVENT = GROUP.registerEvent("revert.task")
    private val CHECK_TASK_EVENT = GROUP.registerEvent("check.task", enumField<CheckStatus>("status"))
    private val REVIEW_STAGE_TOPICS_EVENT = GROUP.registerEvent("review.stage.topics")
    private val HINT_CLICKED_EVENT = GROUP.registerEvent("hint", enumField<HintEvent>(EVENT))
    private val CREATE_COURSE_PREVIEW_EVENT = GROUP.registerEvent("create.course.preview")
    private val PREVIEW_TASK_FILE_EVENT = GROUP.registerEvent("preview.task.file")
    private val CREATE_COURSE_ARCHIVE_EVENT = GROUP.registerEvent("create.course.archive")
    private val POST_COURSE_EVENT = GROUP.registerEvent("post.course", enumField<PostCourseEvent>(EVENT))
    private val SYNCHRONIZE_COURSE_EVENT = GROUP.registerEvent("synchronize.course",
                                                               ITEM_TYPE_FIELD, enumField<SynchronizeCoursePlace>(SOURCE))
    private val IMPORT_COURSE_EVENT = GROUP.registerEvent("import.course")
    private val CODEFORCES_SUBMIT_SOLUTION_EVENT = GROUP.registerEvent("codeforces.submit.solution")
    private val TWITTER_DIALOG_SHOWN_EVENT = GROUP.registerEvent("twitter.dialog.shown", ITEM_TYPE_FIELD, LANGUAGE_FIELD)
    private val COURSE_SELECTION_VIEW_OPENED_EVENT = GROUP.registerEvent("open.course.selection.view",
                                                                         enumField<CourseSelectionViewSource>(SOURCE))
    private val COURSE_SELECTION_TAB_SELECTED_EVENT = GROUP.registerEvent("select.tab.course.selection.view",
                                                                          enumField<CourseSelectionViewTab>(EDU_TAB))
    private val VIEW_EVENT = GROUP.registerEvent("open.task", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)


    @JvmStatic
    fun taskNavigation(place: TaskNavigationPlace) = TASK_NAVIGATION_EVENT.log(place)

    @JvmStatic
    fun eduProjectCreated(course: Course) = EDU_PROJECT_CREATED_EVENT.log(course.courseMode, course.itemType, course.languageID)

    @JvmStatic
    fun eduProjectOpened(course: Course) = EDU_PROJECT_OPENED_EVENT.log(course.courseMode, course.itemType)

    @JvmStatic
    fun studyItemCreated(item: StudyItem) = STUDY_ITEM_CREATED_EVENT.log(item.course.courseMode, item.itemType)

    @JvmStatic
    fun linkClicked(linkType: LinkType) = LICK_CLICKED_EVENT.log(linkType)

    @JvmStatic
    fun loggedIn(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(AuthorizationEvent.LOG_IN, platform, place)

    @JvmStatic
    fun loggedOut(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(AuthorizationEvent.LOG_OUT, platform, place)

    @JvmStatic
    fun fullOutputShown() = SHOW_FULL_OUTPUT_EVENT.log()

    @JvmStatic
    fun solutionPeeked() = PEEK_SOLUTION_EVENT.log()

    @JvmStatic
    fun leaveFeedback() = LEAVE_FEEDBACK_EVENT.log()

    @JvmStatic
    fun revertTask() = REVERT_TASK_EVENT.log()

    @JvmStatic
    fun reviewStageTopics() = REVIEW_STAGE_TOPICS_EVENT.log()

    @JvmStatic
    fun checkTask(status: CheckStatus) = CHECK_TASK_EVENT.log(status)

    @JvmStatic
    fun hintExpanded() = HINT_CLICKED_EVENT.log(HintEvent.EXPANDED)

    @JvmStatic
    fun hintCollapsed() = HINT_CLICKED_EVENT.log(HintEvent.COLLAPSED)

    @JvmStatic
    fun createCoursePreview() = CREATE_COURSE_PREVIEW_EVENT.log()

    @JvmStatic
    fun previewTaskFile() = PREVIEW_TASK_FILE_EVENT.log()

    @JvmStatic
    fun createCourseArchive() = CREATE_COURSE_ARCHIVE_EVENT.log()

    @JvmStatic
    fun updateCourse() = POST_COURSE_EVENT.log(PostCourseEvent.UPDATE)

    @JvmStatic
    fun uploadCourse() = POST_COURSE_EVENT.log(PostCourseEvent.UPLOAD)

    @JvmStatic
    fun synchronizeCourse(course: Course, place: SynchronizeCoursePlace) = SYNCHRONIZE_COURSE_EVENT.log(course.itemType, place)

    @JvmStatic
    fun importCourseArchive() = IMPORT_COURSE_EVENT.log()

    @JvmStatic
    fun codeforcesSubmitSolution() = CODEFORCES_SUBMIT_SOLUTION_EVENT.log()

    @JvmStatic
    fun twitterDialogShown(course: Course) = TWITTER_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageID)

    @JvmStatic
    fun courseSelectionViewOpened(actionPlace: String) {
      COURSE_SELECTION_VIEW_OPENED_EVENT.log(CourseSelectionViewSource.fromActionPlace(actionPlace))
    }

    @JvmStatic
    fun courseSelectionTabSelected(provider: CoursesPlatformProvider) {
      COURSE_SELECTION_TAB_SELECTED_EVENT.log(CourseSelectionViewTab.fromProvider(provider))
    }

    @JvmStatic
    fun viewEvent(task: Task?) {
      val course = task?.course ?: return
      VIEW_EVENT.log(course.courseMode, course.itemType)
    }
  }
}
