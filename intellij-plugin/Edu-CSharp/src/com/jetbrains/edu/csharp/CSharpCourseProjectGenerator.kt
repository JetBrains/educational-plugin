package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.util.io.createDirectories
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import com.jetbrains.rider.services.security.TrustedSolutionStore
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.io.path.pathString

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  private val solutionFileName = "${course.name}.${SolutionFileType.defaultExtension}"

  override fun applySettings(projectSettings: CSharpProjectSettings) {
    super.applySettings(projectSettings)
    course.languageVersion = projectSettings.version
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CSharpProjectSettings, onConfigurationFinished: () -> Unit) {
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)

    val course = project.course ?: error("No course corresponding to the project")

    val tasks = listOf(
      course.lessons.flatMap { it.taskList },
      course.sections.flatMap { it.lessons.flatMap { l -> l.taskList } }
    ).flatten()
    CSharpBackendService.getInstance(project).addCSProjectFilesToSolution(tasks)

    val topLevelDirectories = course.sections + course.lessons.filter { it.getDir(project.courseDir)?.parent == project.courseDir }
    val filesToIndex = topLevelDirectories.map { pathByStudyItem(project, it).toIOFile() }
    CSharpBackendService.getInstance(project).includeFilesToCourseView(filesToIndex)
  }

  /**
   * In order to open a Rider-project, we need to set up an artificial project directory, which is 'solutionDir\.idea\.idea.SolutionName'
   *  Reference can be found in the `ultimate` repo: [com.jetbrains.rider.projectView.SolutionManager#openIdeaProject]
   */
  override fun setUpProjectLocation(location: Path): Path {
    val baseIdeaDir = location.resolve(".idea")
    val projectDir = baseIdeaDir.resolve(".idea.${location.name}")

    TrustedSolutionStore.getInstance().assumeTrusted(projectDir)

    if (projectDir.notExists()) {
      projectDir.createDirectories()
    }
    return projectDir
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    if (!isNewCourse) return
    val content = GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE, mapOf())

    GeneratorUtils.createTextChildFile(
      holder, holder.courseDir,
      solutionFileName,
      content
    )
  }

  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val description = SolutionDescriptionFactory.existing(
      "${location.pathString}/$solutionFileName"
    )
    val strategy = RdOpenSolution(description, true)
    SolutionInitializer.initSolution(it, strategy)
  }
}