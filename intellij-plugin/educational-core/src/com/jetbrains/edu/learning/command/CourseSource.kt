package com.jetbrains.edu.learning.command

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
enum class CourseSource(val option: String, val description: String) {

  ARCHIVE("archive", "Path to course archive file") {
    override suspend fun loadCourse(courseArchivePath: String): Result<Course, String> {
      val course = EduUtilsKt.getLocalCourse(courseArchivePath)
      return if (course != null) Ok(course) else Err("Failed to create course object from `$courseArchivePath` archive")
    }
  },

  MARKETPLACE("marketplace", """Marketplace course link. Supported formats:
    - %course-id%
    - %course-id%-%plugin-name%
    - https://plugins.jetbrains.com/plugin/%course-id%
    - https://plugins.jetbrains.com/plugin/%course-id%-%plugin-name%.
    
    So, for https://plugins.jetbrains.com/plugin/16630-introduction-to-python course, you can pass:
    - 16630
    - 16630-introduction-to-python
    - https://plugins.jetbrains.com/plugin/16630
    - https://plugins.jetbrains.com/plugin/16630-introduction-to-python
  """.trimIndent()) {
    override suspend fun loadCourse(marketplaceCourseLink: String): Result<Course, String> {
      val course = MarketplaceConnector.getInstance().getCourseInfoByLink(marketplaceCourseLink, searchPrivate = true)
      return if (course != null) Ok(course) else Err("Failed to load Marketplace course `$marketplaceCourseLink`")
    }
  },

  HYPERSKILL("hyperskill", "Hyperskill project id") {
    override suspend fun loadCourse(value: String): Result<Course, String> {
      val projectId = value.toIntOrNull() ?: return Err("Hyperskill course id should be integer. Got `$value`")

      val hyperskillProject = HyperskillConnector.getInstance().getProject(projectId)
        .onError { return Err(it) }
      val request = HyperskillOpenProjectStageRequest(projectId, null)
      val hyperskillCourse = HyperskillOpenInIdeRequestHandler
        .createHyperskillCourse(request, hyperskillProject.language, hyperskillProject)
        .onError { return Err(it.message) }
      HyperskillConnector.getInstance().loadStages(hyperskillCourse)

      return Ok(hyperskillCourse)
    }
  };

  abstract suspend fun loadCourse(value: String): Result<Course, String>
}

suspend fun Args.loadCourse(): Result<Course, String> {
  for (courseSource in CourseSource.values()) {
    val value = getOptionValue(courseSource.option) ?: continue
    return withContext(Dispatchers.IO) {
       courseSource.loadCourse(value)
    }
  }

  return Err("Failed to find course source where it should be loaded from")
}
