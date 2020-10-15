package com.jetbrains.edu.rust.actions

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.testFramework.LightProjectDescriptor
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.model.impl.TestCargoProjectsServiceImpl
import org.rust.lang.RsLanguage

class RsCreateTaskTest : RsActionTestBase() {

  override fun getProjectDescriptor(): LightProjectDescriptor = RsProjectDescriptor

  override fun createCourse() {
    // Should be synchronized with workspace structure provided by RsProjectDescriptor
    courseWithFiles(
      courseMode = CCUtils.COURSE_MODE,
      language = RsLanguage,
      settings = RsProjectSettings()
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("main.rs")
        }
      }
      lesson("lesson2")
      additionalFile("Cargo.toml")
    }
  }

  fun `test create new task`() = doTest("task2", shouldBeCreated = true)
  fun `test do not create new task with invalid name`() = doTest("12345", shouldBeCreated = false)
  fun `test do not create new task if name is already used in workspace`() = doTest("task1", shouldBeCreated = false)

  private fun doTest(newTaskName: String, shouldBeCreated: Boolean) {
    val mockUi = MockNewStudyItemUi(newTaskName)
    withMockCreateStudyItemUi(mockUi) {
      testAction(dataContext(findFile("lesson2")), CCCreateTask())
    }

    val task = project.course!!.getLesson("lesson2")!!.getTask(newTaskName)
    if (shouldBeCreated) {
      check(mockUi.errorMessage == null)
      check(task != null)
    }
    else {
      check(mockUi.errorMessage != null)
      check(task == null)
    }
  }
}

// Should be synchronized with createCourse()
private object RsProjectDescriptor : RsProjectDescriptorBase() {

  override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
    super.configureModule(module, model, contentEntry)
    val projectDir = contentEntry.file!!
    val ws = testCargoProject(projectDir.url)
    (module.project.cargoProjects as TestCargoProjectsServiceImpl).createTestProject(projectDir, ws, null)
  }
}
