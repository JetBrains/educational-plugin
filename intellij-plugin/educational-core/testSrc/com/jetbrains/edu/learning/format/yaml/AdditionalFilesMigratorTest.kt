package com.jetbrains.edu.learning.format.yaml

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.vfs.findOrCreateFile
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.writeText
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.CourseReopeningTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.stepik.api.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class AdditionalFilesMigratorTest(
  private val courseMode: CourseMode
) : CourseReopeningTestBase<EmptyProjectSettings>() {
  override val defaultSettings = EmptyProjectSettings

  @Test
  fun `opening course with yaml version 1 without a list of additional files`() {
    val course = course(courseMode = courseMode) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("hello.txt", InMemoryTextualContents("hello.txt"))
        }
      }
    }

    openStudentProjectThenReopenStudentProject(course,
      { project ->
        // make YAML files to be of version 1, when additional files are not in course-info.yaml
        val mode = if (courseMode == CourseMode.STUDENT) "mode: Study" else ""
        createFile(
          "course-info.yaml", """
          type: marketplace
          title: Test course
          language: English
          programming_language: Plain text
          content:
            - lesson1
          yaml_version: 1
          $mode
          """
        )
        createFile(
          "lesson1/lesson-info.yaml", """
          content:
            - task1
          """
        )
        createFile(
          "lesson1/task1/task-info.yaml", """
          type: edu
          files:
            - name: src/Task.kt
              visible: true
            - name: test/Tests.kt
              visible: false
              propagatable: false
          """
        )

        createFile("additional_file1.txt")
        createFile("additional_file2.txt")
        createFile("lesson1/additional_file3.txt")
        createFile("dir/additional_file4.txt")

        // ignore file additional_file2.txt
        createFile(
          ".courseignore", """
          *file2.txt
        """
        )

        // file in the task folder should be ignored
        createFile("lesson1/task1/additional_file5.txt")
      },
      { project ->
        assertEquals(courseMode, project.course?.courseMode)

        // In the unit test mode, YAML files are written only if project.getUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY) == true
        // But we are not able to set it for the project while it is being created, so we just write config files again
        createConfigFiles(project)

        val migratedYaml = project.courseDir.findChild("course-info.yaml")!!.readText()
        val tree = YAMLMapper().readTree(migratedYaml)
        val additionalFilesList = tree.get(ADDITIONAL_FILES).map { it.get(NAME).asText() }

        assertEquals(
          "Wrong list of additional files",
          setOf("additional_file1.txt", "lesson1/additional_file3.txt", "dir/additional_file4.txt"),
          additionalFilesList.toSet()
        )
      }
    )
  }

  private fun createFile(path: String, contents: String = "") = runWriteActionAndWait {
    project.courseDir.findOrCreateFile(path).writeText(contents.trimIndent())
  }

  companion object {
    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Any> {

      return listOf(
        CourseMode.STUDENT,
        CourseMode.EDUCATOR
      )
    }
  }
}