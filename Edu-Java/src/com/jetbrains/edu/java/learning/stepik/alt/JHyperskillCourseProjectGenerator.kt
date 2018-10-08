package com.jetbrains.edu.java.learning.stepik.alt

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.PROJECT_PLAYGROUND
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.generation.EduGradleUtils
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.learning.stepik.alt.HyperskillSettings
import com.jetbrains.edu.learning.stepik.alt.getLesson
import org.jetbrains.plugins.gradle.util.GradleConstants

class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase,
                                        course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)
    GeneratorUtils.runInWriteActionAndWait(
      ThrowableComputable {
        val playgroundDir = VfsUtil.createDirectoryIfMissing(baseDir, PROJECT_PLAYGROUND)
        VfsUtil.createDirectoryIfMissing(playgroundDir, EduNames.SRC)

        val template = FileTemplateManager.getInstance(project).getInternalTemplate(PLAYGROUND_SETTINGS_GRADLE)
        GeneratorUtils.createChildFile(playgroundDir, GradleConstants.SETTINGS_FILE_NAME, template.text)

        val templateBuildGradleText = EduGradleUtils.getInternalTemplateText(
          PLAYGROUND_BUILD_GRADLE, mapOf("GRADLE_VERSION" to EduGradleUtils.gradleVersion())) ?: return@ThrowableComputable
        GeneratorUtils.createChildFile(playgroundDir, GradleConstants.DEFAULT_SCRIPT_NAME, templateBuildGradleText)

      })
  }

  override fun beforeProjectGenerated(): Boolean {
    return try {
      val language = myCourse.languageById
      val lessonId = HyperskillSettings.instance.account?.userInfo?.project?.lesson ?: return false

      val lesson = getLesson(lessonId, language) ?: return false
      myCourse.addLesson(lesson)
      true
    }
    catch (e: Exception) {
      LOG.warn(e)
      false
    }
  }

  companion object {
    @JvmStatic
    private val LOG = Logger.getInstance(JHyperskillCourseProjectGenerator::class.java)
  }
}
