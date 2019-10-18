package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.cpp.checker.CppTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.EducationalCoreIcons
import javax.swing.Icon

class CppCatchConfigurator : CppBaseConfigurator("task.cpp", "catch_test.cpp")

class CppGTestConfigurator : CppBaseConfigurator("task.cpp", "test.cpp")

open class CppBaseConfigurator(
  taskTemplateName: String = "",
  testTemplateName: String = ""
) : EduConfiguratorWithSubmissions<CppProjectSettings>() {

  private val builder: CppCourseBuilder = CppCourseBuilder(taskTemplateName, testTemplateName)
  private val taskCheckerProvider: CppTaskCheckerProvider = CppTaskCheckerProvider()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

  override fun getTestFileName(): String = TEST_CPP

  override fun getMockFileName(text: String): String = TASK_CPP

  override fun getCourseBuilder(): EduCourseBuilder<CppProjectSettings> = builder

  override fun getSourceDir(): String = EduNames.SRC

  override fun getTestDirs(): List<String> = listOf(EduNames.TEST)

  override fun getMockTemplate(): String = getInternalTemplateText(MOCK_CPP)

  override fun isCourseCreatorEnabled(): Boolean = true

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, file)) {
      return true
    }

    val courseDir = project.course?.getDir(project) ?: return false
    // we could use it how indicator because CLion generate build dirs with names `cmake-build-*`
    // @see com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace.getProfileGenerationDirNames
    val buildDirPrefix = GeneratorUtils.joinPaths(courseDir.path, "cmake-build-")
    val googleTestDirPrefix = GeneratorUtils.joinPaths(courseDir.path, TEST_FRAMEWORK_DIR)

    return file.path.startsWith(buildDirPrefix) || file.path.startsWith(googleTestDirPrefix)
  }

  override fun getLogo(): Icon = EducationalCoreIcons.CppLogo

  companion object {
    private const val TASK_CPP = "task.cpp"
    const val TEST_CPP = "test.cpp"
    private const val MOCK_CPP = "mock.cpp"
  }
}