package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.impl.NewProjectUtil
import com.intellij.ide.util.newProjectWizard.AddModuleWizard
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportBuilder
import org.jetbrains.plugins.gradle.service.project.wizard.GradleProjectImportProvider
import java.io.File
import java.io.IOException

open class GradleCourseProjectGenerator(
  private val myCourseBuilder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(course) {

  override fun createProject(location: String, projectSettings: Any): Project? {
    val locationFile = File(FileUtil.toSystemDependentName(location))
    if (!locationFile.exists() && !locationFile.mkdirs()) return null

    val baseDir = WriteAction.compute<VirtualFile, RuntimeException> {
      LocalFileSystem.getInstance().refreshAndFindFileByIoFile(locationFile)
    }
    if (baseDir == null) {
      LOG.error("Couldn't find '$locationFile' in VFS")
      return null
    }
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)

    val isGradleFilesCreated = WriteAction.compute<Boolean, RuntimeException> {
      try {
        EduGradleModuleGenerator.createProjectGradleFiles(location, locationFile.name, myCourseBuilder.buildGradleTemplateName)
        true
      } catch (e: IOException) {
        LOG.error("Failed to generate project with gradle", e)
        false
      }
    }
    if (!isGradleFilesCreated) return null

    val projectDataManager = ProjectDataManager.getInstance()
    val gradleProjectImportBuilder = GradleProjectImportBuilder(projectDataManager)
    val gradleProjectImportProvider = GradleProjectImportProvider(gradleProjectImportBuilder)
    val wizard = AddModuleWizard(null, baseDir.path, gradleProjectImportProvider)
    val project = NewProjectUtil.createFromWizard(wizard, null) ?: return null

    createCourseStructure(project, baseDir, projectSettings as JdkProjectSettings)
    return project
  }

  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: JdkProjectSettings) {
    runWriteAction {
      try {
        val course = GeneratorUtils.initializeCourse(project, myCourse)
        if (CCUtils.isCourseCreator(project) && course.getLessons(true).isEmpty()) {
          val lesson = CCCreateLesson().createAndInitItem(course, null, EduNames.LESSON + 1, 1)
          course.addLesson(lesson)
          val task = CCCreateTask().createAndInitItem(course, lesson, EduNames.TASK + 1, 1)
          lesson.addTask(task)
          myCourseBuilder.initNewTask(task)
        }
        GeneratorUtils.createCourse(course, baseDir)

        setJdk(project, settings)
      } catch (e: IOException) {
        LOG.error("Failed to generate course", e)
      }
    }
  }

  private fun setJdk(project: Project, settings: JdkProjectSettings) {
    val jdk = getJdk(settings)

    // Try to apply model, i.e. commit changes from sdk model into ProjectJdkTable
    try {
      settings.model.apply()
    } catch (e: ConfigurationException) {
      LOG.error(e)
    }

    runWriteAction { ProjectRootManager.getInstance(project).projectSdk = jdk }
  }

  private fun getJdk(settings: JdkProjectSettings): Sdk? {
    val selectedItem = settings.jdkItem ?: return null
    if (selectedItem is JdkComboBox.SuggestedJdkItem) {
      val type = selectedItem.sdkType
      val path = selectedItem.path
      val jdkRef = Ref<Sdk>()
      settings.model.addSdk(type, path, Consumer<Sdk> { jdkRef.set(it) })
      return jdkRef.get()
    }
    return selectedItem.jdk
  }

  companion object {

    private val LOG = Logger.getInstance(GradleCourseProjectGenerator::class.java)
  }
}
