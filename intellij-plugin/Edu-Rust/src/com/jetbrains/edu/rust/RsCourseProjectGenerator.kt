package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.openapiext.pathAsPath

class RsCourseProjectGenerator(builder: RsCourseBuilder, course: Course) :
  CourseProjectGenerator<RsProjectSettings>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: RsProjectSettings, onConfigurationFinished: () -> Unit) {
    project.rustSettings.modify {
      it.toolchain = projectSettings.toolchain
    }

    if (!project.isSingleWorkspaceProject) {
      course.visitLessons {
        for (task in it.taskList) {
          val manifestFile = task.getDir(project.courseDir)?.findChild(CargoConstants.MANIFEST_FILE) ?: continue
          project.cargoProjects.attachCargoProject(manifestFile.pathAsPath)
        }
      }
    }
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    if (!isNewCourse) return

    val members = mutableListOf<String>()
    holder.course.visitLessons { lesson ->
      val lessonDir = lesson.getDir(holder.courseDir) ?: return@visitLessons
      val lessonDirPath = VfsUtil.getRelativePath(lessonDir, holder.courseDir) ?: return@visitLessons
      members += "    \"${lessonDirPath}/*/\""
    }

    val initialMembers = members.joinToString(",\n", postfix = if (members.isEmpty()) "" else ",")

    GeneratorUtils.createFileFromTemplate(holder, "Cargo.toml", "workspaceCargo.toml", mapOf(INITIAL_MEMBERS to initialMembers))
  }

  companion object {
    private const val INITIAL_MEMBERS = "INITIAL_MEMBERS"
  }
}
