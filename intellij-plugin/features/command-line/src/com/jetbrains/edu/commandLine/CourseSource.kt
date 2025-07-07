package com.jetbrains.edu.commandLine

import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
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
  },

  MARKETPLACE("marketplace", "Marketplace course id") {
    override suspend fun loadCourse(value: String): Result<Course, String> {
      val courseId = value.toIntOrNull() ?: return Err("Marketplace course id should be an integer. Got `$value`")
      val course = MarketplaceConnector.getInstance().searchCourse(courseId, searchPrivate = true)
      return if (course != null) Ok(course) else Err("Failed to load Marketplace course `$value`")
    }
  },

  HYPERSKILL("hyperskill", "Hyperskill project id") {
    override suspend fun loadCourse(value: String): Result<Course, String> {
      val projectId = value.toIntOrNull() ?: return Err("Hyperskill course id should be an integer. Got `$value`")

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
