package com.jetbrains.edu.learning.statistics

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem

object EduCounterUsageCollector {
  enum class TaskNavigationPlace {
    CHECK_ALL_NOTIFICATION,
    TASK_DESCRIPTION_TOOLBAR,
    CHECK_PANEL,
    UNRESOLVED_DEPENDENCY_NOTIFICATION
  }

  @JvmStatic
  fun taskNavigation(place: TaskNavigationPlace) =
    reportEvent("navigate.to.task", mapOf(SOURCE to place.toLower()))

  @JvmStatic
  fun eduProjectCreated(mode: String) = reportEvent("edu.project.created", mapOf(MODE to mode))

  @JvmStatic
  fun eduProjectOpened(mode: String) = reportEvent("edu.project.opened", mapOf(MODE to mode))

  @JvmStatic
  fun studyItemCreated(item: StudyItem) =
    reportEvent("study.item.created", mapOf(MODE to item.course.courseMode, TYPE to item.itemType))

  enum class LinkType {
    IN_COURSE, STEPIK, EXTERNAL, PSI
  }

  @JvmStatic
  fun linkClicked(linkType: LinkType) = reportEvent("link.clicked", mapOf("linkType" to linkType.toLower()))

  private enum class AuthorizationEvent {
    LOG_IN, LOG_OUT
  }

  enum class AuthorizationPlace {
    SETTINGS, WIDGET, START_COURSE_DIALOG
  }

  private fun authorization(event: AuthorizationEvent, platform: String, place: AuthorizationPlace) =
    reportEvent("authorization", mapOf(EVENT to event.toLower(), "platform" to platform, SOURCE to place.toLower()))

  @JvmStatic
  fun loggedIn(platform: String, place: AuthorizationPlace) =
    authorization(EduCounterUsageCollector.AuthorizationEvent.LOG_IN, platform, place)

  @JvmStatic
  fun loggedOut(platform: String, place: AuthorizationPlace) =
    authorization(EduCounterUsageCollector.AuthorizationEvent.LOG_OUT, platform, place)

  @JvmStatic
  fun fullOutputShown() = reportEvent("show.full.output")

  @JvmStatic
  fun solutionPeeked() = reportEvent("peek.solution")

  @JvmStatic
  fun leaveFeedback() = reportEvent("leave.feedback")

  @JvmStatic
  fun revertTask() = reportEvent("revert.task")

  @JvmStatic
  fun reviewStageTopics() = reportEvent("review.stage.topics")

  @JvmStatic
  fun checkTask(status: CheckStatus) = reportEvent("check.task", mapOf("status" to status.toLower()))

  private enum class HintEvent {
    EXPANDED, COLLAPSED
  }

  private fun hintClicked(event: HintEvent) = reportEvent("hint", mapOf(EVENT to event.toLower()))

  @JvmStatic
  fun hintExpanded() = hintClicked(EduCounterUsageCollector.HintEvent.EXPANDED)

  @JvmStatic
  fun hintCollapsed() = hintClicked(EduCounterUsageCollector.HintEvent.COLLAPSED)

  fun createCoursePreview() = reportEvent("create.course.preview")

  @JvmStatic
  fun previewTaskFile() = reportEvent("preview.task.file")

  @JvmStatic
  fun createCourseArchive() = reportEvent("create.course.archive")

  private enum class PostCourseEvent {
    UPLOAD, UPDATE
  }

  private fun postCourse(event: PostCourseEvent) = reportEvent("post.course", mapOf(EVENT to event.toLower()))

  @JvmStatic
  fun updateCourse() = postCourse(PostCourseEvent.UPDATE)

  @JvmStatic
  fun uploadCourse() = postCourse(PostCourseEvent.UPLOAD)

  enum class SynchronizeCoursePlace {
    WIDGET, PROJECT_GENERATION, PROJECT_REOPEN
  }

  @JvmStatic
  fun synchronizeCourse(place: SynchronizeCoursePlace) = reportEvent("synchronize.course", mapOf(SOURCE to place.toLower()))

  @JvmStatic
  fun importCourseArchive() = reportEvent("import.course")

  private fun Enum<*>.toLower() = this.toString().toLowerCase()

  const val GROUP_ID = "educational.counters"
  private const val MODE = "mode"
  private const val SOURCE = "source"
  private const val EVENT = "event"
  private const val TYPE = "type"
}