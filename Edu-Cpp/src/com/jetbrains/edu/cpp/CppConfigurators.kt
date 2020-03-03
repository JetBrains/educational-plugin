package com.jetbrains.edu.cpp

import com.google.common.annotations.VisibleForTesting
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

class CppGTestConfigurator : CppBaseConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCourseBuilder(TASK_CPP, TEST_CPP)
}

class CppCatchConfigurator : CppBaseConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCourseBuilder(TASK_CPP, CATCH_TEST_CPP)

  companion object {
    @VisibleForTesting
    const val CATCH_TEST_CPP = "catch_test.cpp"
  }
}

open class CppBaseConfigurator : EduConfiguratorWithSubmissions<CppProjectSettings>() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCourseBuilder("", "")

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppTaskCheckerProvider()

  override val testFileName: String
    get() = TEST_CPP

  override fun getMockFileName(text: String): String = TASK_CPP

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_CPP)

  override val isCourseCreatorEnabled: Boolean
    get() = true

  override val logo: Icon
    get() = EducationalCoreIcons.CppLogo

  override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, file)) {
      return true
    }

    val courseDir = project.course?.getDir(project) ?: return false
    // we could use it how indicator because CLion generate build dirs with names `cmake-build-*`
    // @see com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace.getProfileGenerationDirNames
    val buildDirPrefix = GeneratorUtils.joinPaths(courseDir.path, "cmake-build-")
    val googleTestDirPrefix = GeneratorUtils.joinPaths(courseDir.path, TestFrameworks.baseDir.value)

    return file.path.startsWith(buildDirPrefix) || file.path.startsWith(googleTestDirPrefix)
  }

  companion object {
    const val TASK_CPP = "task.cpp"
    const val TEST_CPP = "test.cpp"
    private const val MOCK_CPP = "mock.cpp"
  }
}