package com.jetbrains.edu.commandLine

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.jetbrains.edu.coursecreator.archive.CourseArchiveCreator
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

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
  },

  LOCAL("local", "Path to local educator course project") {
    // TODO: can we just open project here and use its own course instead of creating a new one?
    override suspend fun loadCourse(projectPath: String): Result<Course, String> {
      val projectPath = Paths.get(projectPath).absolute()
      if (!projectPath.exists()) return Err("$projectPath does not exist")
      val project = ProjectManagerEx.getInstanceEx().openProjectAsync(projectPath, OpenProjectTask {
        isNewProject = false
      }) ?: return Err("Failed to open project at `$projectPath`")

      try {
        val course = project.course ?: return Err("Failed to load local course `$projectPath`")
        if (course.isStudy) return Err("Local course should be in educator mode")

        val archiveFile = Files.createTempFile("course_archive_", ".zip")
        val archiveCreationError = withContext(Dispatchers.EDT) {
          CourseArchiveCreator(project, archiveFile).createArchive(course)
        }
        if (archiveCreationError != null) {
          return Err("Failed to create course archive: ${archiveCreationError.message}")
        }

        val courseFromArchive = withContext(Dispatchers.IO) {
          EduUtilsKt.getLocalCourse(archiveFile.absolutePathString())
        }

        return if (courseFromArchive == null) {
          Err("Failed to create course object from `$archiveFile` archive")
        }
        else {
          Ok(courseFromArchive)
        }
      }
      finally {
        runCatching { ProjectManagerEx.getInstanceEx().forceCloseProjectAsync(project) }
      }
    }

    override fun isCourseFromSource(course: Course): Boolean = false
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
