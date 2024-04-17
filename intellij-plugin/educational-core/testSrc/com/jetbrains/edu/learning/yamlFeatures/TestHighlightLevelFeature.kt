package com.jetbrains.edu.learning.yamlFeatures

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile
import com.intellij.psi.PsiManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.junit.Test

class TestHighlightLevelFeature : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  @Test
  fun `test highlight level NONE disables highlighting per file`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1-no-highlight") {
          taskFile("Main1.java") {
            withHighlightLevel(EduFileErrorHighlightLevel.NONE)
          }
        }
        eduTask("task2") {
          taskFile("Main2.java")
        }
      }
      frameworkLesson("lesson2-framework") {
        eduTask("task1") {
          taskFile("Main1.java")
        }
        eduTask("task2-no-highlight") {
          taskFile("Main1.java") {
            withHighlightLevel(EduFileErrorHighlightLevel.NONE)
          }
        }
        eduTask("task3") {
          taskFile("Main1.java")
        }
      }
    }

    createCourseStructure(course)
    // wait for the project to be initialized
    UIUtil.dispatchAllInvocationEvents()

    val highlightManager = HighlightingSettingsPerFile.getInstance(project)
    val psiManager = PsiManager.getInstance(project)

    fun assertHighlightLevel(path: String, highlightLevel: EduFileErrorHighlightLevel) {
      val virtualFile = findFile(path)
      val actualHighlightLevel = highlightManager.getHighlightingSettingForRoot(
        psiManager.findFile(virtualFile) ?: error("failed to find psi file $path")
      )
      assertEquals(highlightLevel == EduFileErrorHighlightLevel.NONE, actualHighlightLevel == FileHighlightingSetting.SKIP_HIGHLIGHTING)
    }

    assertHighlightLevel("lesson1/task1-no-highlight/Main1.java", EduFileErrorHighlightLevel.NONE)
    assertHighlightLevel("lesson1/task2/Main2.java", EduFileErrorHighlightLevel.ALL_PROBLEMS)

    // framework lesson
    val frameworkLesson = course.getLesson("lesson2-framework")!!
    val task1 = frameworkLesson.taskList[0]
    val task2 = frameworkLesson.taskList[1]
    val task3 = frameworkLesson.taskList[2]

    assertHighlightLevel("lesson2-framework/task/Main1.java", EduFileErrorHighlightLevel.ALL_PROBLEMS)
    NavigationUtils.navigateToTask(project, task2, task1)
    assertHighlightLevel("lesson2-framework/task/Main1.java", EduFileErrorHighlightLevel.NONE)
    NavigationUtils.navigateToTask(project, task3, task2)
    assertHighlightLevel("lesson2-framework/task/Main1.java", EduFileErrorHighlightLevel.ALL_PROBLEMS)
  }
}