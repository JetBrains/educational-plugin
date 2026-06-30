package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.environment.JdkLanguageEnvironment
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.gradleSanitizeName
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.SbtVersion

class ScalaSbtCourseProjectGenerator(builder: ScalaSbtCourseBuilder, course: Course) : CourseProjectGenerator<JdkLanguageEnvironment>(
  builder,
  course
) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val sbtVersion = maxOf(SbtVersion.`Latest$`.`MODULE$`.Sbt_1().value(), MIN_RECOMMENDED_SBT_VERSION)
    val templateVariables = mapOf(
      PROJECT_NAME to gradleSanitizeName(holder.courseDir.name),
      "SBT_VERSION" to sbtVersion.toString()
    )

    return listOf(
      GeneratorUtils.createFromInternalTemplateOrFromDisk(holder.courseDir, BUILD_SBT, BUILD_SBT, templateVariables),
      GeneratorUtils.createFromInternalTemplateOrFromDisk(
        holder.courseDir,
        "${Sbt.ProjectDirectory()}/${Sbt.PropertiesFile()}",
        Sbt.PropertiesFile(),
        templateVariables
      )
    )
  }

  override suspend fun prepareToOpen(project: Project, module: Module) {
    super.prepareToOpen(project, module)
    @Suppress("UnstableApiUsage")
    writeAction { GeneratorUtils.removeModule(project, module) }
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
  }

  companion object {
    // Minimal version of sbt that supports java 13
    private val MIN_RECOMMENDED_SBT_VERSION: Version = Version("1.3.3")
  }
}
