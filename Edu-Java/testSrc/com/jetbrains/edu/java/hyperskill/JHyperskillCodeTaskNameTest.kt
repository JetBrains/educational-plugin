package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JHyperskillCodeTaskNameTest : EduTestCase() {
  fun `test find taskFile for uploading`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse,
      settings = JdkProjectSettings.emptySettings()
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.java", """
            public class Task {
            }
          """.trimIndent())
          taskFile("src/CoolTaskName.java", """
            public class CoolTaskName {
              public static void main(String[] args) {
              }
            }
          """.trimIndent())
          taskFile("test/Tests1.java")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val task = findTask(0, 0)
    val codeTaskFile = course.configurator?.getCodeTaskFile(project, task)

    assertEquals("src/CoolTaskName.java", codeTaskFile!!.name)
  }

  fun `test create name for taskfile`() {
    val course = courseWithFiles(
      language = JavaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse,
      settings = JdkProjectSettings.emptySettings()
    ) {} as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val fileName = course.configurator?.getMockFileName("""
      public class CoolTaskName {
        public static void main(String[] args) {
        }
      }
      """.trimIndent())

    assertEquals("CoolTaskName.java", fileName)
  }
}
