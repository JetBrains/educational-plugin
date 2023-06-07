package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.actionSystem.ActionPlaces
import com.jetbrains.edu.learning.checkio.CheckiOPlatformProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.coursera.CourseraPlatformProvider
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationEvent.*
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.JetBrainsAcademyPlatformProvider

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
    IN_COURSE, STEPIK, EXTERNAL, PSI, CODEFORCES, JBA, FILE
  }

  /**
   * [LOG_IN] and [LOG_OUT] events describe user _clicked_ link to do log in or log out
   * These events were used before 2022.3 plugin version
   *
   * [LOG_IN_SUCCEED] and [LOG_OUT_SUCCEED] events describe user actually did authorized or logged out
   */
  private enum class AuthorizationEvent {
    @Deprecated("Use LOG_IN_SUCCEED instead")
    LOG_IN,
    @Deprecated("Use LOG_OUT_SUCCEED instead")
    LOG_OUT,
    LOG_IN_SUCCEED, LOG_OUT_SUCCEED
  }

  enum class AuthorizationPlace {
    SETTINGS, WIDGET, START_COURSE_DIALOG, SUBMISSIONS_TAB, TASK_DESCRIPTION_HEADER, UNKNOWN
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
  private enum class CourseActionSource(private val actionPlace: String? = null) {
    WELCOME_SCREEN(ActionPlaces.WELCOME_SCREEN),
    MAIN_MENU(ActionPlaces.MAIN_MENU),
    FIND_ACTION(ActionPlaces.ACTION_SEARCH),
    COURSE_SELECTION_DIALOG(BrowseCoursesDialog.ACTION_PLACE),
    UNKNOWN;

    companion object {
      fun fromActionPlace(actionPlace: String): CourseActionSource {
        // it is possible to have action place like "popup@WelcomScreen"
        val actionPlaceParsed = actionPlace.split("@").last()
        return values().firstOrNull { it.actionPlace == actionPlaceParsed } ?: UNKNOWN
      }
    }
  }

  private enum class CourseSelectionViewTab {
    MARKETPLACE,
    JBA,
    CHECKIO,
    CODEFORCES,
    COURSERA,
    MY_COURSES,
    UNKNOWN;

    companion object {
      fun fromProvider(provider: CoursesPlatformProvider): CourseSelectionViewTab {
        return when (provider) {
          is JetBrainsAcademyPlatformProvider -> JBA
          is CheckiOPlatformProvider -> CHECKIO
          is CodeforcesPlatformProvider -> CODEFORCES
          is CourseraPlatformProvider -> COURSERA
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

    private val GROUP = EventLogGroup("educational.counters", 8)

    private val COURSE_MODE_FIELD = EventFields.Enum<CourseMode>(MODE)
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
                                                    listOf("JAVA", "kotlin", "Python", "Scala",
                                                           "JavaScript", "Rust", "ObjectiveC", "go", "PHP"))


    private val TASK_NAVIGATION_EVENT = GROUP.registerEvent("navigate.to.task", enumField<TaskNavigationPlace>(SOURCE))
    private val EDU_PROJECT_CREATED_EVENT = GROUP.registerEvent("edu.project.created", COURSE_MODE_FIELD, ITEM_TYPE_FIELD, LANGUAGE_FIELD)
    private val EDU_PROJECT_OPENED_EVENT = GROUP.registerEvent("edu.project.opened", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)
    private val STUDY_ITEM_CREATED_EVENT = GROUP.registerEvent("study.item.created", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)
    private val LICK_CLICKED_EVENT = GROUP.registerEvent("link.clicked", enumField<LinkType>(TYPE))
    private val AUTHORIZATION_EVENT = GROUP.registerEvent("authorization",
                                                          enumField<AuthorizationEvent>(EVENT),
                                                          EventFields.String("platform", listOf(
                                                            "Hyperskill",
                                                            "Stepik",
                                                            "Js_CheckiO",
                                                            "Py_CheckiO",
                                                            "Marketplace",
                                                            "Codeforces"
                                                          )),
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
                                                                         enumField<CourseActionSource>(SOURCE))
    private val COURSE_SELECTION_TAB_SELECTED_EVENT = GROUP.registerEvent("select.tab.course.selection.view",
                                                                          enumField<CourseSelectionViewTab>(EDU_TAB))
    private val VIEW_EVENT = GROUP.registerEvent("open.task", COURSE_MODE_FIELD, ITEM_TYPE_FIELD)
    private val CREATE_NEW_COURSE_CLICK_EVENT = GROUP.registerEvent("create.new.course.clicked",
                                                                    enumField<CourseActionSource>(SOURCE))


    @JvmStatic
    fun taskNavigation(place: TaskNavigationPlace) = TASK_NAVIGATION_EVENT.log(place)

    @JvmStatic
    fun eduProjectCreated(course: Course) = EDU_PROJECT_CREATED_EVENT.log(course.courseMode, course.itemType, course.languageId)

    @JvmStatic
    fun eduProjectOpened(course: Course) = EDU_PROJECT_OPENED_EVENT.log(course.courseMode, course.itemType)

    @JvmStatic
    fun studyItemCreated(item: StudyItem) = STUDY_ITEM_CREATED_EVENT.log(item.course.courseMode, item.itemType)

    @JvmStatic
    fun linkClicked(linkType: LinkType) = LICK_CLICKED_EVENT.log(linkType)

    @JvmStatic
    @Deprecated("Use logInSucceed instead")
    fun loggedIn(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN, platform, place)

    @JvmStatic
    @Deprecated("Use logOutSucceed instead")
    fun loggedOut(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT, platform, place)

    @JvmStatic
    fun logInSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN_SUCCEED, platform, place)

    @JvmStatic
    fun logOutSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT_SUCCEED, platform, place)

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
    fun twitterDialogShown(course: Course) = TWITTER_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageId)

    @JvmStatic
    fun courseSelectionViewOpened(actionPlace: String) {
      COURSE_SELECTION_VIEW_OPENED_EVENT.log(CourseActionSource.fromActionPlace(actionPlace))
    }

    @JvmStatic
    fun courseSelectionTabSelected(provider: CoursesPlatformProvider) {
      COURSE_SELECTION_TAB_SELECTED_EVENT.log(CourseSelectionViewTab.fromProvider(provider))
    }

    fun createNewCourseClicked(actionPlace: String) {
      CREATE_NEW_COURSE_CLICK_EVENT.log(CourseActionSource.fromActionPlace(actionPlace))
    }

    @JvmStatic
    fun viewEvent(task: Task?) {
      val course = task?.course ?: return
      VIEW_EVENT.log(course.courseMode, course.itemType)
    }
  }
}
