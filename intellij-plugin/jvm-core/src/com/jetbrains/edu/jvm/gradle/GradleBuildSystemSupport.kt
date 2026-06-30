package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.application.EDT
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.jvm.environment.JdkBuildSystemSupport
import com.jetbrains.edu.jvm.environment.JdkVersionRange
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.setGradleSettings
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.jvmcompat.GradleJvmSupportMatrix

object GradleBuildSystemSupport : JdkBuildSystemSupport {
  override suspend fun configureProject(project: Project, course: Course, jdk: Sdk) {
    withContext(Dispatchers.EDT) {
      setGradleSettings(project, project.basePath!!) { gradleProjectSettings ->
        gradleProjectSettings.gradleJvm = USE_PROJECT_JDK
      }
    }
  }

  override fun getJdkVersionRange(course: Course): Result<JdkVersionRange, String> {
    val gradleVersionFromCourse = EduGradleUtils.detectGradleVersion(course)

    val gradleVersion = gradleVersionFromCourse ?: if (course.courseMode == CourseMode.EDUCATOR) {
      // let educators use the latest gradle version
      GradleVersion.current()
    }
    else {
      // learners should use a legacy version, because only old courses do not specify the gradle version
      GradleVersion.version(GradleCourseBuilderBase.LEGACY_GRADLE_VERSION)
    }

    // if the gradle version is newer than the current one, we probably don't know what JDK version is supported,
    // and the GradleJvmSupportMatrix fails to determine it
    val latestJdkVersion = if (gradleVersion > GradleVersion.current()) {
      null
    }
    else {
      GradleJvmSupportMatrix.suggestLatestSupportedJavaVersion(gradleVersion)?.feature
    }

    return Ok(
      JdkVersionRange(
        GradleJvmSupportMatrix.suggestOldestSupportedJavaVersion(gradleVersion)?.feature,
        latestJdkVersion
      )
    )
  }
}