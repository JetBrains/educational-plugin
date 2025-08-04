package com.jetbrains.edu.commandLine

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseStorage.api.CourseStorageConnector
import com.jetbrains.edu.learning.marketplace.isFromCourseStorage
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
enum class CourseSource(val option: String, val description: String) {

  ARCHIVE("archive", "Path to course archive file") {
    override suspend fun loadCourse(courseArchivePath: String): Result<Course, String> {
      val course = EduUtilsKt.getLocalCourse(courseArchivePath)
      return if (course != null) Ok(course) else Err("Failed to create course object from `$courseArchivePath` archive")
    }

    override fun isCourseFromSource(course: Course): Boolean = false
  },

  MARKETPLACE("marketplace", "Marketplace course id") {
    override suspend fun loadCourse(location: String): Result<Course, String> {
      val courseId = location.toIntOrNull() ?: return Err("Marketplace course id should be an integer. Got `$location`")

      val marketplaceConnector = MarketplaceConnector.getInstance()
      val course = marketplaceConnector.searchCourse(courseId, searchPrivate = false) ?:
        marketplaceConnector.searchCourse(courseId, searchPrivate = true)

      return if (course != null) Ok(course) else Err("Failed to load Marketplace course `$location`")
    }

    override fun isCourseFromSource(course: Course): Boolean =
      course is EduCourse && course.isMarketplaceRemote && !course.isFromCourseStorage()
  },

  COURSE_STORAGE("courseStorage", "Course id from course storage") {
    override suspend fun loadCourse(location: String): Result<Course, String> {
      val courseId = location.toIntOrNull() ?: return Err("Course id from storage should be an integer. Got `$location`")

      val connector = CourseStorageConnector.getInstance()
      val course = connector.searchCourse(courseId, searchPrivate = false) ?: connector.searchCourse(courseId, searchPrivate = true)

      return if (course != null) Ok(course) else Err("Failed to load course from the course storage `$location`")
    }

    override fun isCourseFromSource(course: Course): Boolean = course is EduCourse && course.isFromCourseStorage()
  },

  HYPERSKILL("hyperskill", "Hyperskill project id") {
    override suspend fun loadCourse(location: String): Result<Course, String> {
      val projectId = location.toIntOrNull() ?: return Err("Hyperskill course id should be an integer. Got `$location`")

      val hyperskillProject = HyperskillConnector.getInstance().getProject(projectId)
        .onError { return Err(it) }
      val request = HyperskillOpenProjectStageRequest(projectId, null)
      val hyperskillCourse = HyperskillOpenInIdeRequestHandler
        .createHyperskillCourse(request, hyperskillProject.language, hyperskillProject)
        .onError { return Err(it.message) }
      HyperskillConnector.getInstance().loadStages(hyperskillCourse)

      return Ok(hyperskillCourse)
    }

    override fun isCourseFromSource(course: Course): Boolean = course is HyperskillCourse
  };

  /**
   * Loads course from this source by course [location]
   * where a particular format of location depends on a particular source
   */
  abstract suspend fun loadCourse(location: String): Result<Course, String>

  /**
   * Checks if given [course] is came from this source or not
   */
  abstract fun isCourseFromSource(course: Course): Boolean
}
