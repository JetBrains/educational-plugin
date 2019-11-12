package com.jetbrains.edu.go

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class GoCourseProjectGenerator(builder: GoCourseBuilder, course: Course) :
  CourseProjectGenerator<GoProjectSettings>(builder, course) {
  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    val modFileName = "go.mod"
    GeneratorUtils.createChildFile(baseDir, modFileName, getInternalTemplateText(modFileName))
  }
}
