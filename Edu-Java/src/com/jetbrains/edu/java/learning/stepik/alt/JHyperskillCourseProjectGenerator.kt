package com.jetbrains.edu.java.learning.stepik.alt

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames.PROJECT_PLAYGROUND
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.alt.HyperskillConnector
import java.io.IOException

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)
    try {
      VfsUtil.createDirectoryIfMissing(PROJECT_PLAYGROUND)
    }
    catch (e: IOException) {
      LOG.warn("Failed to create project playground")
    }
  }

  override fun beforeProjectGenerated(): Boolean {
    return try {
      val language = myCourse.languageById
      val sections = HyperskillConnector.getSections(language.displayName)
      sections.forEach { section -> myCourse.addSection(section) }
      true
    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  companion object {
    @JvmStatic
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
