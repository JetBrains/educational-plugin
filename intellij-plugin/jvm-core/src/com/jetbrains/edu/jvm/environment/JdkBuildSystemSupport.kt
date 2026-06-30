package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course

/**
 * Different JVM build systems require their own additional installation steps, and they also require special JVM versions
 */
interface JdkBuildSystemSupport {
  suspend fun configureProject(project: Project, course: Course, jdk: Sdk)
  fun getJdkVersionRange(course: Course): Result<JdkVersionRange, String>
}