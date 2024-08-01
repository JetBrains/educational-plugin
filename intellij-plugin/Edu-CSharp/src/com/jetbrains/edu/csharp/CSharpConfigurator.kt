package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.pathRelativeToTask
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class CSharpConfigurator : EduConfigurator<CSharpProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpCourseBuilder()

  override val testFileName: String
    get() = TEST_CS

  override val taskCheckerProvider: TaskCheckerProvider
    get() = TODO("Not yet implemented")

  override fun excludeFromArchive(project: Project, course: Course, file: VirtualFile): Boolean {
    if (super.excludeFromArchive(project, course, file)) return true
    return file.pathRelativeToTask(project).contains(BIN_DIRECTORY) || file.pathRelativeToTask(project).contains(OBJ_DIRECTORY)
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.CSHARP_COURSES)

  override fun getMockFileName(course: Course, text: String): String = TASK_CS
  override val logo: Icon
    get() = EducationalCoreIcons.CSharpLogo

  companion object {
    @NonNls
    const val TASK_CS = "Task.cs"

    @NonNls
    const val MAIN_CS = "Program.cs"

    @NonNls
    const val TEST_CS = "Test.cs"

    const val BIN_DIRECTORY = "bin/"
    const val OBJ_DIRECTORY = "obj/"
  }
}