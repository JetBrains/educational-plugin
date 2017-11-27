package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.builders.ModuleFixtureBuilder
import com.intellij.testFramework.fixtures.CodeInsightFixtureTestCase
import com.jetbrains.edu.learning.courseFormat.Course

abstract class CourseGenerationTestBase<Settings> : CodeInsightFixtureTestCase<ModuleFixtureBuilder<*>>() {

  abstract val courseBuilder: EduCourseBuilder<Settings>
  abstract val defaultSettings: Settings

  protected val rootDir: VirtualFile get() = project.baseDir

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  protected fun <Settings> createCourseStructure(builder: EduCourseBuilder<Settings>, course: Course, settings: Settings) {
    val generator = builder.getCourseProjectGenerator(course) ?: error("given builder returns null as course project generator")
    generator.beforeProjectGenerated()
    generator.generateProject(myFixture.project, rootDir, settings, myModule)
    generator.afterProjectGenerated(myFixture.project, settings)
  }

  protected fun generateCourseStructure(pathToCourseJson: String): Course {
    val course = createCourseFromJson(pathToCourseJson)
    createCourseStructure(courseBuilder, course, defaultSettings)
    return course
  }
}
