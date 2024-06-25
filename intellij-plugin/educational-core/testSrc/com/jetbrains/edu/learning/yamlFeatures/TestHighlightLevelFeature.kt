package com.jetbrains.edu.learning.yamlFeatures

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingSettingsPerFile
import com.intellij.testFramework.utils.vfs.getPsiFile
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.waitUntilIndexesAreReady
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
    waitUntilIndexesAreReady(project)

    assertHighlightLevel("lesson1/task1-no-highlight/Main1.java", FileHighlightingSetting.SKIP_HIGHLIGHTING)
    assertHighlightLevel("lesson1/task2/Main2.java", FileHighlightingSetting.FORCE_HIGHLIGHTING)

    // framework lesson
    val frameworkLesson = course.getLesson("lesson2-framework")!!
    val task1 = frameworkLesson.taskList[0]
    val task2 = frameworkLesson.taskList[1]
    val task3 = frameworkLesson.taskList[2]

    assertHighlightLevel("lesson2-framework/task/Main1.java", FileHighlightingSetting.FORCE_HIGHLIGHTING)
    NavigationUtils.navigateToTask(project, task2, task1)
    assertHighlightLevel("lesson2-framework/task/Main1.java", FileHighlightingSetting.SKIP_HIGHLIGHTING)
    NavigationUtils.navigateToTask(project, task3, task2)
    assertHighlightLevel("lesson2-framework/task/Main1.java", FileHighlightingSetting.FORCE_HIGHLIGHTING)
  }

  private fun assertHighlightLevel(path: String, expectedHighlightLevel: FileHighlightingSetting) {
    val file = findFile(path).getPsiFile(project)
    val actualHighlightLevel = HighlightingSettingsPerFile.getInstance(project).getHighlightingSettingForRoot(file)
    assertEquals("Unexpected highlighting level for $path", expectedHighlightLevel, actualHighlightLevel)
  }
}
