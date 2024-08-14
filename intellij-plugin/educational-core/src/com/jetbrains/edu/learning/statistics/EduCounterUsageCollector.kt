package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.actionSystem.ActionPlaces
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
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
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_ID_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_MODE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.ITEM_TYPE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.LANGUAGE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.PLATFORM_FIELD
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.HyperskillPlatformProvider

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
    IN_COURSE, STEPIK, EXTERNAL, PSI, CODEFORCES, JBA, FILE, IFT
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
    COURSERA,
    MY_COURSES,
    UNKNOWN;

    companion object {
      fun fromProvider(provider: CoursesPlatformProvider): CourseSelectionViewTab {
        return when (provider) {
          is HyperskillPlatformProvider -> JBA
          is CourseraPlatformProvider -> COURSERA
          is MarketplacePlatformProvider -> MARKETPLACE
          is MyCoursesProvider -> MY_COURSES
          else -> UNKNOWN
        }
      }
    }
  }

  companion object {
    private const val SOURCE = "source"
    private const val SUCCESS = "success"
    private const val EVENT = "event"
    private const val TYPE = "type"
    private const val EDU_TAB = "tab"

    private val GROUP = EventLogGroup(
      "educational.counters",
      "The metric is reported in case a user has called the corresponding JetBrains Academy features.",
      19,
    )

    private val TASK_NAVIGATION_EVENT = GROUP.registerEvent(
      "navigate.to.task",
      "The event is recorded in case a user navigates to the next or previous task/stage/problem.",
      enumField<TaskNavigationPlace>(SOURCE)
    )
    private val EDU_PROJECT_CREATED_EVENT = GROUP.registerEvent(
      "edu.project.created",
      "The event is recorded in case a user creates a new course.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val EDU_PROJECT_OPENED_EVENT = GROUP.registerEvent(
      "edu.project.opened",
      "The event is recorded in case a user opens an already existing course.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD
    )
    private val CC_STUDY_ITEM_CREATED_EVENT = GROUP.registerEvent(
      "study.item.created",
      "The event is recorded in case a new study item (different types of lessons and tasks) is created.",
      ITEM_TYPE_FIELD,
      COURSE_ID_FIELD,
      PLATFORM_FIELD
    )
    private val CC_TASK_CREATED_EVENT = GROUP.registerEvent(
      "task.created",
      "Event is logged whenever a new task is created within a lesson, indicating the type of the lesson.",
      ITEM_TYPE_FIELD,
      COURSE_ID_FIELD,
      PLATFORM_FIELD
    )
    private val LICK_CLICKED_EVENT = GROUP.registerEvent(
      "link.clicked",
      "The event is recorded in case a user clicks a link within a task text.",
      enumField<LinkType>(TYPE)
    )
    private val AUTHORIZATION_EVENT = GROUP.registerEvent(
      "authorization",
      "The event is recorded in case a user logs in or out on any platform we support.",
      enumField<AuthorizationEvent>(EVENT),
      EventFields.String(
        "platform", listOf(
          "Hyperskill",
          "Stepik",
          "Js_CheckiO",
          "Py_CheckiO",
          "Marketplace",
          "Codeforces"
        )
      ),
      enumField<AuthorizationPlace>(SOURCE)
    )
    private val OBTAIN_JBA_TOKEN_EVENT = GROUP.registerEvent(
      "obtain.jba.token",
      "The event is recorded in case a request for a JetBrains account access token is processed.",
      EventFields.Boolean(SUCCESS)
    )
    private val SHOW_FULL_OUTPUT_EVENT = GROUP.registerEvent(
      "show.full.output",
      "The event is recorded in case a user clicks the Show Full Output link in the check result panel."
    )
    private val PEEK_SOLUTION_EVENT = GROUP.registerEvent(
      "peek.solution",
      "The event is recorded in case a user clicks the Peek Solution link in the check result panel."
    )
    private val LEAVE_FEEDBACK_EVENT = GROUP.registerEvent(
      "leave.feedback",
      "The event is recorded in case a user clicks the Leave Feedback icon in the check result panel."
    )
    private val REVERT_TASK_EVENT = GROUP.registerEvent(
      "revert.task",
      "The event is recorded in case a user successfully resets content of a task."
    )
    private val CHECK_TASK_EVENT = GROUP.registerEvent(
      "check.task",
      "The event is recorded in case a user checks a task in any course.",
      enumField<CheckStatus>("status")
    )
    private val RATE_MARKETPLACE_COURSE = GROUP.registerEvent(
      "rate.marketplace.course",
      "The event is recorded in case a user clicks on the Rate Course icon in the Task Description."
    )
    private val REVIEW_STAGE_TOPICS_EVENT = GROUP.registerEvent(
      "review.stage.topics",
      "The event is recorded in case a user clicks Review Topics for a stage in a JetBrains Academy project."
    )
    private val HINT_CLICKED_EVENT = GROUP.registerEvent(
      "hint",
      "The event is recorded in case a user expands/collapses hints in the Task Description.",
      enumField<HintEvent>(EVENT)
    )
    private val CREATE_COURSE_PREVIEW_EVENT = GROUP.registerEvent(
      "create.course.preview",
      "The event is recorded in case an educator creates a course preview for a course in Course Creation mode."
    )
    private val PREVIEW_TASK_FILE_EVENT = GROUP.registerEvent(
      "preview.task.file",
      "The event is recorded in case an educator uses Preview for a task file in Course Creation mode."
    )
    private val CREATE_COURSE_ARCHIVE_EVENT = GROUP.registerEvent(
      "create.course.archive",
      "The event is recorded in case an educator generates an archive for their course in Course Creation mode."
    )
    private val POST_COURSE_EVENT = GROUP.registerEvent(
      "post.course",
      "The event is recorded in case an educator uploads or updates their course on Stepik or Marketplace in Course Creation mode.",
      enumField<PostCourseEvent>(EVENT)
    )
    private val SYNCHRONIZE_COURSE_EVENT = GROUP.registerEvent(
      "synchronize.course",
      "The event is recorded in case a course is synchronized with its latest version.",
      ITEM_TYPE_FIELD,
      enumField<SynchronizeCoursePlace>(SOURCE)
    )
    private val IMPORT_COURSE_EVENT = GROUP.registerEvent(
      "import.course",
      "The event is recorded in case a user opens a course from a disk in Learner mode."
    )
    private val CODEFORCES_SUBMIT_SOLUTION_EVENT = GROUP.registerEvent(
      "codeforces.submit.solution",
      "The event is recorded in case a user clicks Submit Solution in the Task Description for a Codeforces task."
    )
    private val TWITTER_DIALOG_SHOWN_EVENT = GROUP.registerEvent(
      "twitter.dialog.shown",
      "The event is recorded in case a user receives a suggestion to tweet about completing a course or task (e.g., JB Academy project completion).",
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val LINKEDIN_DIALOG_SHOWN_EVENT = GROUP.registerEvent(
      "linkedin.dialog.shown",
      "The event is recorded in case a user receives a suggestion to post to LinkedIn about completing a course or task (e.g., JB Academy project completion).",
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val COURSE_SELECTION_VIEW_OPENED_EVENT = GROUP.registerEvent(
      "open.course.selection.view",
      "The event is recorded in case a user opens the Course Selection view.",
      enumField<CourseActionSource>(SOURCE)
    )
    private val COURSE_SELECTION_TAB_SELECTED_EVENT = GROUP.registerEvent(
      "select.tab.course.selection.view",
      "The event is recorded in case a user selects a tab in the Course Selection view.",
      enumField<CourseSelectionViewTab>(EDU_TAB)
    )
    private val VIEW_EVENT = GROUP.registerEvent(
      "open.task",
      "The event is recorded in case a user opens any task.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD
    )
    private val CREATE_NEW_COURSE_CLICK_EVENT = GROUP.registerEvent(
      "create.new.course.clicked",
      "The event is recorded in case a user opens the Create Course dialog.",
      enumField<CourseActionSource>(SOURCE)
    )
    private val CREATE_NEW_FILE_IN_NON_TEMPLATE_BASED_FRAMEWORK_LESSON_BY_LEARNER =
      GROUP.registerEvent(
        "create.new.file.in.non.template.based.framework.lesson.by.learner",
        "The event is recorded in case a user creates a new file in a Non-Template Based Framework Lesson in Learner mode."
      )

    private val SOLUTION_SHARING_PROMPT_EVENT = GROUP.registerEvent(
      "submission.share.invite.shown",
      "The event is recorded in case a user enables the Solution Sharing banner/notification."
    )

    private val COMMUNITY_SOLUTION_DIFF_OPENED = GROUP.registerEvent(
      "peer.solution.diff.opened",
      "The event is recorded in case a user opens the Community Solution diff."
    )

    private val SUBMISSION_SUCCESS = GROUP.registerEvent(
      "submission.attempt",
      "The event is recorded in case a user makes a submission attempt, regardless of whether it fails or succeeds.",
      EventFields.Boolean(SUCCESS)
    )

    private val AGREE_TO_ENABLE_INVITE_ACTION = GROUP.registerEvent(
      "submission.invite.action",
      "The event is recorded in case a user responds to the feature invitation, regardless of whether it is accepted or declined.",
      EventFields.Boolean(SUCCESS)
    )

    private val AGREE_TO_ENABLE_SOLUTION_SHARE_STATE = GROUP.registerEvent(
      "solution.share.state",
      "The event is recorded in case a user's sharing preference is updated.",
      EventFields.Boolean(SUCCESS)
    )

    private val OPEN_COMMUNITY_TAB = GROUP.registerEvent(
      "open.community.tab",
      "The event is recorded in case a user opens the Community panel on the Submissions tab to explore other learners' solutions."
    )

    fun taskNavigation(place: TaskNavigationPlace) = TASK_NAVIGATION_EVENT.log(place)

    fun eduProjectCreated(course: Course) = EDU_PROJECT_CREATED_EVENT.log(course.courseMode, course.itemType, course.languageId)

    fun eduProjectOpened(course: Course) = EDU_PROJECT_OPENED_EVENT.log(course.courseMode, course.itemType)

    fun studyItemCreatedCC(item: StudyItem) {
      val course = item.course
      if (CourseMode.EDUCATOR == course.courseMode) {
        CC_STUDY_ITEM_CREATED_EVENT.log(item.itemType, course.id, course.itemType)
        if (item is Task) {
          CC_TASK_CREATED_EVENT.log(item.lesson.itemType, course.id, course.itemType)
        }
      }
    }

    fun linkClicked(linkType: LinkType) = LICK_CLICKED_EVENT.log(linkType)

    @Deprecated("Use logInSucceed instead")
    fun loggedIn(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN, platform, place)

    @Deprecated("Use logOutSucceed instead")
    fun loggedOut(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT, platform, place)

    fun logInSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN_SUCCEED, platform, place)

    fun logOutSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT_SUCCEED, platform, place)

    fun obtainJBAToken(success: Boolean) = OBTAIN_JBA_TOKEN_EVENT.log(success)

    fun fullOutputShown() = SHOW_FULL_OUTPUT_EVENT.log()

    fun solutionPeeked() = PEEK_SOLUTION_EVENT.log()

    fun leaveFeedback() = LEAVE_FEEDBACK_EVENT.log()

    fun rateMarketplaceCourse() = RATE_MARKETPLACE_COURSE.log()

    fun revertTask() = REVERT_TASK_EVENT.log()

    fun reviewStageTopics() = REVIEW_STAGE_TOPICS_EVENT.log()

    fun checkTask(status: CheckStatus) = CHECK_TASK_EVENT.log(status)

    fun hintExpanded() = HINT_CLICKED_EVENT.log(HintEvent.EXPANDED)

    fun hintCollapsed() = HINT_CLICKED_EVENT.log(HintEvent.COLLAPSED)

    fun createCoursePreview() = CREATE_COURSE_PREVIEW_EVENT.log()

    fun previewTaskFile() = PREVIEW_TASK_FILE_EVENT.log()

    fun createCourseArchive() = CREATE_COURSE_ARCHIVE_EVENT.log()

    fun updateCourse() = POST_COURSE_EVENT.log(PostCourseEvent.UPDATE)

    fun uploadCourse() = POST_COURSE_EVENT.log(PostCourseEvent.UPLOAD)

    fun synchronizeCourse(course: Course, place: SynchronizeCoursePlace) = SYNCHRONIZE_COURSE_EVENT.log(course.itemType, place)

    fun importCourseArchive() = IMPORT_COURSE_EVENT.log()

    fun codeforcesSubmitSolution() = CODEFORCES_SUBMIT_SOLUTION_EVENT.log()

    fun twitterDialogShown(course: Course) = TWITTER_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageId)

    fun linkedInDialogShown(course: Course) = LINKEDIN_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageId)

    fun courseSelectionViewOpened(actionPlace: String) {
      COURSE_SELECTION_VIEW_OPENED_EVENT.log(CourseActionSource.fromActionPlace(actionPlace))
    }

    fun courseSelectionTabSelected(provider: CoursesPlatformProvider) {
      COURSE_SELECTION_TAB_SELECTED_EVENT.log(CourseSelectionViewTab.fromProvider(provider))
    }

    fun createNewCourseClicked(actionPlace: String) {
      CREATE_NEW_COURSE_CLICK_EVENT.log(CourseActionSource.fromActionPlace(actionPlace))
    }

    fun createNewFileInNonTemplateBasedFrameworkLessonByLearner() = CREATE_NEW_FILE_IN_NON_TEMPLATE_BASED_FRAMEWORK_LESSON_BY_LEARNER.log()

    fun viewEvent(task: Task?) {
      val course = task?.course ?: return
      VIEW_EVENT.log(course.courseMode, course.itemType)
    }

    fun solutionSharingPromptShown() = SOLUTION_SHARING_PROMPT_EVENT.log()

    fun communitySolutionDiffOpened() = COMMUNITY_SOLUTION_DIFF_OPENED.log()

    fun submissionSuccess(result: Boolean) = SUBMISSION_SUCCESS.log(result)

    fun solutionSharingInviteAction(action: Boolean) = AGREE_TO_ENABLE_INVITE_ACTION.log(action)

    fun solutionSharingState(state: Boolean) = AGREE_TO_ENABLE_SOLUTION_SHARE_STATE.log(state)

    fun openCommunityTab() = OPEN_COMMUNITY_TAB.log()
  }
}
