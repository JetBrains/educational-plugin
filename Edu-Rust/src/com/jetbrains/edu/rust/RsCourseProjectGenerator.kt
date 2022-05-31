package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.openapiext.pathAsPath

class RsCourseProjectGenerator(builder: RsCourseBuilder, course: Course) :
  CourseProjectGenerator<RsProjectSettings>(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: RsProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
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
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
    if (!isNewCourse) return
    val course = project.course ?: return

    val members = mutableListOf<String>()
    course.visitLessons { lesson ->
      val lessonDir = lesson.getDir(project.courseDir) ?: return@visitLessons
      val lessonDirPath = VfsUtil.getRelativePath(lessonDir, baseDir) ?: return@visitLessons
      members += "    \"${lessonDirPath}/*/\""
    }

    val initialMembers = members.joinToString(",\n", postfix = if (members.isEmpty()) "" else ",")

    GeneratorUtils.createFileFromTemplate(project, baseDir, "Cargo.toml", "workspaceCargo.toml", mapOf(INITIAL_MEMBERS to initialMembers))
  }

  companion object {
    private const val INITIAL_MEMBERS = "INITIAL_MEMBERS"
  }
}
