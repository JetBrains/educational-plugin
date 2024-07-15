package com.jetbrains.edu.rust.actions

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ContentEntry
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.Urls
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.junit.Test
import org.rust.cargo.CfgOptions
import org.rust.cargo.project.model.CargoProjectsService
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.project.workspace.CargoWorkspaceData
import org.rust.cargo.project.workspace.PackageOrigin
import org.rust.lang.RsLanguage
import java.nio.file.Paths

class RsCreateTaskTest : RsActionTestBase() {

  override fun getProjectDescriptor(): LightProjectDescriptor = RsProjectDescriptor

  override fun createCourse() {
    // Should be synchronized with workspace structure provided by RsProjectDescriptor
    courseWithFiles(
      courseMode = CourseMode.EDUCATOR,
      language = RsLanguage
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

  @Test
  fun `test create new task`() = doTest("task2", shouldBeCreated = true)
  @Test
  fun `test do not create new task with invalid name`() = doTest("12345", shouldBeCreated = false)
  @Test
  fun `test do not create new task if name is already used in workspace`() = doTest("task1", shouldBeCreated = false)

  private fun doTest(newTaskName: String, shouldBeCreated: Boolean) {
    val mockUi = MockNewStudyItemUi(newTaskName)
    withMockCreateStudyItemUi(mockUi) {
      testAction(CCCreateTask.ACTION_ID, dataContext(findFile("lesson2")))
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
private object RsProjectDescriptor : LightProjectDescriptor() {

  override fun configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry) {
    super.configureModule(module, model, contentEntry)
    val projectDir = contentEntry.file!!
    val ws = testCargoProject(projectDir.url)
    module.project.cargoProjects.createTestProject(projectDir, ws)
  }

  private fun testCargoProject(contentRoot: String): CargoWorkspace {
    val packages = listOf(testCargoPackage(contentRoot))
    return CargoWorkspace.deserialize(Paths.get("${Urls.newFromIdea(contentRoot).path}/Cargo.toml"),
                                      CargoWorkspaceData(packages, emptyMap(), emptyMap()), cfgOptions = CfgOptions.DEFAULT)
  }

  private fun testCargoPackage(contentRoot: String): CargoWorkspaceData.Package = CargoWorkspaceData.Package(
    id = "task1 0.0.1",
    contentRootUrl = contentRoot,
    name = "task1",
    version = "0.0.1",
    targets = listOf(
      CargoWorkspaceData.Target("$contentRoot/lesson1/task1/main.rs", "task1", CargoWorkspace.TargetKind.Bin,
                                edition = CargoWorkspace.Edition.EDITION_2015, doctest = false, requiredFeatures = emptyList())
    ),
    source = null,
    origin = PackageOrigin.WORKSPACE,
    edition = CargoWorkspace.Edition.EDITION_2018,
    features = emptyMap(),
    enabledFeatures = emptySet(),
    cfgOptions = CfgOptions.EMPTY,
    env = emptyMap(),
    outDirUrl = null,
    categories = emptySet()
  )

  // Rust team moved `TestCargoProjectsServiceImpl` into test sources.
  // As a result, the plugin doesn't contain test implementation class, and it's impossible to use it now.
  //
  // Uncomment implementation when the issue is fixed
  @Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
  private fun CargoProjectsService.createTestProject(projectDir: VirtualFile, ws: CargoWorkspace) {
//    (this as TestCargoProjectsServiceImpl).createTestProject(projectDir, ws, null)
    TODO()
  }
}
