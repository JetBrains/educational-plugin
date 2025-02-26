package com.jetbrains.edu.csharp

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.csharp.checker.CSharpTaskCheckerProvider
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class CSharpConfigurator : EduConfigurator<CSharpProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpCourseBuilder()

  override val testFileName: String
    get() = TEST_CS

  override val sourceDir: String
    get() = SRC_DIRECTORY

  override val testDirs: List<String>
    get() = listOf(TEST_DIRECTORY)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CSharpTaskCheckerProvider()

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    extension(SolutionFileType.defaultExtension) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    dirAndChildren(BIN_DIRECTORY, OBJ_DIRECTORY) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.CSHARP_COURSES)

  override fun getMockFileName(course: Course, text: String): String = TASK_CS
  override val logo: Icon
    get() = EducationalCoreIcons.Language.CSharp

  companion object {
    @NonNls
    const val TASK_CS = "Task.cs"

    @NonNls
    const val TEST_CS = "Test.cs"

    const val SRC_DIRECTORY = "src"
    const val TEST_DIRECTORY = "test"

    const val BIN_DIRECTORY = "bin"
    const val OBJ_DIRECTORY = "obj"
  }
}