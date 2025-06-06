package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.jvmEnvironmentSettings
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.attributesEvaluator.AttributesEvaluator
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.scala.isScalaPluginCompatible
import com.jetbrains.edu.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import com.jetbrains.edu.scala.sbt.checker.ScalaSbtTaskCheckerProvider
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.sbt.Sbt
import javax.swing.Icon

class ScalaSbtConfigurator : EduConfigurator<JdkProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = ScalaSbtCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio() && isScalaPluginCompatible

  override val taskCheckerProvider: TaskCheckerProvider
    get() = ScalaSbtTaskCheckerProvider()

  override fun getMockFileName(course: Course, text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Scala

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dirAndChildren("target") {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(BUILD_SBT) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    dir(Sbt.ProjectDirectory()) {
      file(Sbt.PropertiesFile(), direct = true) {
        archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
      }
    }
  }

  companion object {
    const val TEST_SCALA = "TestSpec.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
