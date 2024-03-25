package com.jetbrains.edu.kotlin.eduAssistant

import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.lang.Language
import com.jetbrains.edu.kotlin.eduAssistant.courses.ijPluginCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.eduAssistant.inspection.InspectionProvider
import org.junit.experimental.categories.Category
import org.junit.runners.Parameterized

@Category(AiAutoQualityCodeTests::class)
class TaskBasedAssistantTestForIJPluginCourse(lesson: String, task: String) : BaseAssistantTest(lesson, task) {

  override val course: Course = ijPluginCourse
  override val language: Language = course.languageById ?: error("Language could not be determined")
  override fun getInspections(language: Language): List<LocalInspectionTool> {
    return InspectionProvider.getInspections(language)
  }
  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data(): Collection<Array<Any>> {
      return listOf(
        /* Test case for lesson 1, task 2: Validates the functionality of counting Kotlin classes in a PSI file.
           This test verifies that the countKtClasses function correctly counts the number of KtClass instances
           within a given Kotlin file, ensuring that the PsiTreeUtil.findChildrenOfType method is utilized effectively
           to find all relevant KtClass elements as described in the lesson's theory. */
        arrayOf("lesson1", "task2"),
        /* Test case for lesson 2, task 2: Validates the functionality of PSI element modifications.
           This test verifies the correct implementation of the sortMethods function, ensuring methods
           within a Kotlin class are sorted alphabetically. This task showcases the application of
           PsiElement.add(), PsiElement.delete(), PsiElement.replace() and PsiElement.copy() for method sorting operations. */
        arrayOf("lesson2", "task2")
      )
    }
  }
}
