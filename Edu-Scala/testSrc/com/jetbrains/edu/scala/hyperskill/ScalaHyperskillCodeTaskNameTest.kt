package com.jetbrains.edu.scala.hyperskill

import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaHyperskillCodeTaskNameTest : EduTestCase() {
  fun `test find taskFile for uploading`() {
    val course = courseWithFiles(
      language = ScalaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse,
      settings = JdkProjectSettings.emptySettings()
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.scala", "object Task {}")
          taskFile("src/CoolTaskName.scala", """
            object CoolTaskName extends App {
              println("Hello, world!")
            }
          """.trimIndent())
          taskFile("test/Tests1.scala")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val task = findTask(0, 0)
    val codeTaskFile = course.configurator?.getCodeTaskFile(project, task)

    assertEquals("src/CoolTaskName.scala", codeTaskFile!!.name)
  }

  fun `test create name for taskfile`() {
    val course = courseWithFiles(
      language = ScalaLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse,
      settings = JdkProjectSettings.emptySettings()
    ) {} as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val fileName = course.configurator?.getMockFileName("""
      object CoolTaskName extends App {
        println("Hello, world!")
      }
      """.trimIndent())

    assertEquals("CoolTaskName.scala", fileName)
  }
}