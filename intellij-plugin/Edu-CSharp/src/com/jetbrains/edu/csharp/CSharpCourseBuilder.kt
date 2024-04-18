package com.jetbrains.edu.csharp

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.intellij.openapi.project.Project

class CSharpCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {

  override fun taskTemplateName(course: Course): String = CSharpConfigurator.TASK_CS
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getDefaultSettings(): Result<CSharpProjectSettings, String> = Ok(CSharpProjectSettings())

  override fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? =
    if (name.matches(STUDY_ITEM_NAME_PATTERN)) null else "error.invalid.name"

  companion object {
    private val LOG: Logger = Logger.getInstance(CSharpCourseBuilder::class.java)

    private val STUDY_ITEM_NAME_PATTERN = "[a-zA-Z0-9_ ]+".toRegex()
  }
}
