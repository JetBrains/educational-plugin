package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.cpp.checker.CppTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfiguratorWithSubmissions
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import icons.CMakeIcons
import javax.swing.Icon

class CppConfigurator : EduConfiguratorWithSubmissions<CppProjectSettings>() {

  private val builder: CppCourseBuilder = CppCourseBuilder()
  private val taskCheckerProvider: CppTaskCheckerProvider = CppTaskCheckerProvider()

  override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

  override fun getTestFileName(): String = TEST_CPP

  override fun getMockFileName(text: String): String = TASK_CPP

  override fun getCourseBuilder(): EduCourseBuilder<CppProjectSettings> = builder

  override fun getSourceDir(): String = EduNames.SRC

  override fun getTestDirs(): List<String> = listOf(EduNames.TEST)

  override fun getMockTemplate(): String = getInternalTemplateText(MOCK_CPP)

  override fun isCourseCreatorEnabled(): Boolean = true

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean =
    super.excludeFromArchive(project, file) || file.path.contains("cmake-build-debug")


  override fun getLogo(): Icon = CMakeIcons.CMake

  override fun isValidItemName(name: String): String? =
    if (name.matches(STUDY_ITEM_NAME_PATTERN)) null else "Name should contain only latin letters, digits, spaces or '_' symbols."

  companion object {
    const val GTEST_VERSION = "release-1.8.1"

    const val TASK_CPP = "task.cpp"
    const val TEST_CPP = "test.cpp"
    private const val MOCK_CPP = "mock.cpp"

    private val STUDY_ITEM_NAME_PATTERN = "[a-zA-Z0-9_ ]+".toRegex()
  }
}