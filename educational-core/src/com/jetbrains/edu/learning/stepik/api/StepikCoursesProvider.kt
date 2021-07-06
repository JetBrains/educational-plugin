package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.stepik.ListedCoursesIdsProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.course.stepikCourseFromRemote
import kotlinx.coroutines.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext


class StepikCoursesProvider : CoroutineScope {

  private val executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

  private val loadedCourses: Deferred<List<EduCourse>> = async { loadAllCourses() }

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.IO

  private suspend fun loadAllCourses(): List<EduCourse> {
    LOG.info("Loading courses started...")
    val startTime = System.currentTimeMillis()

    return coroutineScope {
      val publicCourses = async { getPublicCourseInfos() }
      val privateCourses = async { loadPrivateCourseInfos() }
      val listedStepikCourses = async { loadListedStepikCourses() }

      val result = awaitAll(publicCourses, privateCourses, listedStepikCourses).flatten()

      coroutineScope {
        launch { setAuthors(result) }
        launch { setReviews(result) }
      }

      LOG.info("Loading courses finished...Took " + (System.currentTimeMillis() - startTime) + " ms")
      result
    }
  }

  suspend fun getStepikCourses(): List<StepikCourse> {
    return loadedCourses.await().filterIsInstance<StepikCourse>()
  }

  fun loadPrivateCourseInfos(): List<EduCourse> {
    if (EduSettings.getInstance().user == null) {
      return emptyList()
    }
    val result = mutableListOf<EduCourse>()
    var currentPage = 1
    while (true) {
      val coursesList = StepikConnector.getInstance().getCourses(false, currentPage, true) ?: break

      val availableCourses = getAvailableCourses(coursesList)
      result.addAll(availableCourses)
      currentPage += 1
      if (!coursesList.meta.containsKey("has_next") || coursesList.meta["has_next"] == false) break
    }

    return result
  }

  suspend fun getFeaturedCourses(): List<Course> {
    return loadedCourses.await()
      .filterNot { it is StepikCourse }
      .filter { it.isStepikPublic && it.id in ListedCoursesIdsProvider.featuredCommunityCourses }
      .sortedByDescending { it.reviewScore }
  }

  suspend fun getAllOtherCourses(): List<EduCourse> {
    return loadedCourses.await()
      .filterNot { it is StepikCourse }
      .filter { it.isStepikPublic && it.id !in ListedCoursesIdsProvider.featuredCommunityCourses }
  }

  suspend fun getPrivateCourses(): List<Course> {
    return loadedCourses.await()
      .filterNot { it is StepikCourse }
      .filterNot { it.isStepikPublic }
  }

  private fun getPublicCourseInfos(): List<EduCourse> {
    val indicator = ProgressManager.getInstance().progressIndicator
    val tasks = mutableListOf<Callable<List<EduCourse>?>>()
    val minEmptyPageNumber = AtomicInteger(Integer.MAX_VALUE)

    for (i in 0 until PUBLIC_COURSES_THREADS_NUMBER) {
      tasks.add(Callable {
        val courses = mutableListOf<EduCourse>()
        var pageNumber = i + 1
        while (pageNumber < minEmptyPageNumber.get() && addCoursesFromStepik(courses, true, pageNumber, null, minEmptyPageNumber)) {
          if (indicator != null && indicator.isCanceled) {
            return@Callable null
          }
          pageNumber += PUBLIC_COURSES_THREADS_NUMBER
        }
        return@Callable courses
      })
    }

    val result = mutableListOf<EduCourse>()
    ConcurrencyUtil.invokeAll(tasks, executorService)
      .filterNot { it.isCancelled }
      .mapNotNull { it.get() }
      .forEach { result.addAll(it) }

    return result
  }

  private fun setAuthors(result: List<EduCourse>) {
    val allUsers = StepikConnector.getInstance().getUsers(result)
    val usersById = allUsers.associateBy { it.id }

    for (course in result) {
      val authors = course.instructors.mapNotNull { usersById[it] }
      course.authors = authors
    }
  }

  private fun setReviews(courses: List<EduCourse>) {
    val summaryIds = courses.map { it.reviewSummary }
    val reviewsByCourseId = StepikConnector.getInstance().getCourseReviewSummaries(summaryIds).associateBy { it.courseId }
    for (course in courses) {
      course.reviewScore = reviewsByCourseId[course.id]?.average ?: 0.0
    }
  }

  private fun loadListedStepikCourses(): List<StepikCourse> {
    val listedCoursesIds = ListedCoursesIdsProvider.featuredStepikCourses.keys + ListedCoursesIdsProvider.inProgressCourses
    val courses = StepikConnector.getInstance().getCourses(listedCoursesIds) ?: return emptyList()
    val result = mutableListOf<StepikCourse>()
    courses.forEach { course ->
      val courseId = course.id
      val languages = ListedCoursesIdsProvider.featuredStepikCourses[courseId]

      fun addCourse() {
        val remoteCourse = stepikCourseFromRemote(course) ?: return
        if (ListedCoursesIdsProvider.inProgressCourses.contains(courseId)) {
          remoteCourse.visibility = CourseVisibility.InProgressVisibility(ListedCoursesIdsProvider.inProgressCourses.indexOf(courseId))
        }
        remoteCourse.let { result.add(it) }
      }

      if (languages.isNullOrEmpty()) {
        addCourse()
      }
      else {
        languages.forEach { language ->
          course.language = language
          // as course is copied to become Stepik type, it's ok to use the same loaded course for different languages
          addCourse()
        }
      }
    }

    return result
  }

  companion object {
    private val LOG = logger<StepikCoursesProvider>()

    private const val PUBLIC_COURSES_THREADS_NUMBER = 4
  }
}
