/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.newproject

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.impl.setTrusted
import com.intellij.ide.util.PropertiesComponent
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.FeaturedVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.setUpPluginDependencies
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.Companion.synchronizeCourse
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader.loadCourseStructure
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.io.File
import java.io.IOException
import kotlin.system.measureTimeMillis

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see HyperskillCourseProjectGenerator
 */
abstract class CourseProjectGenerator<S : Any>(
  protected val myCourseBuilder: EduCourseBuilder<S>,
  protected var myCourse: Course
) {
  private var alreadyEnrolled = false

  open fun beforeProjectGenerated(): Boolean {
    if (myCourse !is EduCourse || !myCourse.isStepikRemote) return true
    val remoteCourse = myCourse as EduCourse

    if (remoteCourse.id <= 0) return true
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<Boolean, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        val user = EduSettings.getInstance().user
        if (user != null) {
          alreadyEnrolled = StepikConnector.getInstance().isEnrolledToCourse(remoteCourse.id)
          if (!alreadyEnrolled) {
            StepikConnector.getInstance().enrollToCourse(remoteCourse.id)
          }
        }
        loadCourseStructure(remoteCourse)
        myCourse = remoteCourse
        true
      }, EduCoreBundle.message("generate.project.loading.course.progress.text"), true, null)
  }

  open fun afterProjectGenerated(project: Project, projectSettings: S) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateAllWidgets()

    setUpPluginDependencies(project, myCourse)

    loadSolutions(project, myCourse)
    EduUtils.openFirstTask(myCourse, project)

    YamlFormatSynchronizer.saveAll(project)
    YamlFormatSynchronizer.startSynchronization(project)
  }

  /**
   * Generate new project and create course structure for created project
   *
   * @param location location of new course project
   * @param projectSettings new project settings
   */
  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little bit differently
  // we use Object instead of S and cast to S when it needed
  fun doCreateCourseProject(location: String, projectSettings: Any): Project? {
    if (!beforeProjectGenerated()) {
      return null
    }
    @Suppress("UNCHECKED_CAST")
    val castedProjectSettings = projectSettings as S
    val createdProject = createProject(location, castedProjectSettings) ?: return null
    afterProjectGenerated(createdProject, castedProjectSettings)
    return createdProject
  }

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use [CourseProjectGenerator.createCourseStructure]
   *
   * @param locationString location of new project
   * @param projectSettings new project settings
   * @return project of new course or null if new project can't be created
   */
  private fun createProject(locationString: String, projectSettings: S): Project? {
    val location = File(FileUtil.toSystemDependentName(locationString))
    if (!location.exists() && !location.mkdirs()) {
      val message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.absolutePath)
      Messages.showErrorDialog(message, ActionsBundle.message("action.NewDirectoryProject.title"))
      return null
    }
    val baseDir = WriteAction.compute<VirtualFile?, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location) }
    if (baseDir == null) {
      LOG.error("Couldn't find '$location' in VFS")
      return null
    }
    VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)

    RecentProjectsManager.getInstance().lastProjectCreationLocation = PathUtil.toSystemIndependentName(location.parent)

    baseDir.putUserData(COURSE_MODE_TO_CREATE, myCourse.courseMode)
    baseDir.putUserData(COURSE_LANGUAGE_ID_TO_CREATE, myCourse.languageID)

    val isNewCourseCreatorCourse = isNewCourseCreatorCourse
    if (isNewTrustedProjectApiAvailable && isCourseTrusted(myCourse, isNewCourseCreatorCourse)) {
      setProjectPathTrusted(location.toPath())
    }

    return openNewProject(location.toPath()) { module ->
      createCourseStructure(module.project, module, baseDir, projectSettings)
    }
  }

  /**
   * Create course structure for already created project.
   * @param project course project
   * @param module base project module
   * @param baseDir base directory of project
   * @param settings project settings
   */
  @VisibleForTesting
  open fun createCourseStructure(project: Project, module: Module, baseDir: VirtualFile, settings: S) {
    GeneratorUtils.initializeCourse(project, myCourse)
    val isNewCourseCreatorCourse = isNewCourseCreatorCourse

    // BACKCOMPAT: 2021.3. Drop it because project is marked as trusted in `createProject`
    if (!isNewTrustedProjectApiAvailable && isCourseTrusted(myCourse, isNewCourseCreatorCourse)) {
      @Suppress("UnstableApiUsage")
      project.setTrusted(true)
    }
    if (isNewCourseCreatorCourse) {
      val lesson = myCourseBuilder.createInitialLesson(project, myCourse)
      if (lesson != null) {
        myCourse.addLesson(lesson)
      }
    }

    val indicator = ProgressManager.getInstance().progressIndicator
    try {
      if (indicator == null) {
        ProgressManager.getInstance().runProcessWithProgressSynchronously<Unit, IOException>({
          generateCourseContent(project, baseDir, isNewCourseCreatorCourse, ProgressManager.getInstance().progressIndicator)
        }, EduCoreBundle.message("generate.project.generate.course.structure.progress.text"), false, project)
      } else {
        indicator.text = EduCoreBundle.message("generate.project.generate.course.structure.progress.text")
        generateCourseContent(project, baseDir, isNewCourseCreatorCourse, indicator)
      }
    }
    catch (e: IOException) {
      LOG.error("Failed to generate course", e)
    }
  }

  @Throws(IOException::class)
  private fun generateCourseContent(
    project: Project,
    baseDir: VirtualFile,
    isNewCourseCreatorCourse: Boolean,
    indicator: ProgressIndicator
  ) {
    val duration = measureTimeMillis {
      if (CCUtils.isCourseCreator(project)) {
        CCUtils.initializeCCPlaceholders(project, myCourse)
      }
      GeneratorUtils.createCourse(project, myCourse, baseDir, indicator)
      if (myCourse is EduCourse &&
          (myCourse.isStepikRemote || (myCourse as EduCourse).isMarketplaceRemote) &&
          CCUtils.isCourseCreator(project)) {
        checkIfAvailableOnRemote()
      }
      createAdditionalFiles(project, baseDir, isNewCourseCreatorCourse)
      EduCounterUsageCollector.eduProjectCreated(myCourse)
    }
    LOG.info("Course content generation: $duration ms")
  }

  private fun checkIfAvailableOnRemote() {
    val remoteCourse = if (myCourse.isMarketplace) {
      MarketplaceConnector.getInstance().searchCourse(myCourse.id, myCourse.isMarketplacePrivate)
    }
    else {
      StepikConnector.getInstance().getCourseInfo(myCourse.id, null, true)
    }
    if (remoteCourse == null) {
      val platformName = if (myCourse.isMarketplace) MARKETPLACE else StepikNames.STEPIK
      LOG.warn("Failed to get $platformName course for imported from zip course with id: ${myCourse.id}")
      LOG.info("Converting course to local. Course id: ${myCourse.id}")
      (myCourse as EduCourse).convertToLocal()
    }
  }

  private fun loadSolutions(project: Project, course: Course) {
    if (course.isStudy &&
        course is EduCourse &&
        course.isStepikRemote &&
        EduSettings.isLoggedIn()) {
      PropertiesComponent.getInstance(project).setValue(StepikNames.ARE_SOLUTIONS_UPDATED_PROPERTY, true, false)
      if (alreadyEnrolled) {
        val stepikSolutionsLoader = StepikSolutionsLoader.getInstance(project)
        stepikSolutionsLoader.loadSolutionsInBackground()
        synchronizeCourse(course, EduCounterUsageCollector.SynchronizeCoursePlace.PROJECT_GENERATION)
      }
    }
  }

  /**
   * Creates additional files that are not in course object
   *
   * @param project course project
   * @param baseDir base directory of project
   * @param isNewCourse `true` if course is new one, `false` otherwise
   *
   * @throws IOException
   */
  @Throws(IOException::class)
  open fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {}

  private val isNewCourseCreatorCourse: Boolean
    get() = myCourse.courseMode == CourseMode.EDUCATOR && myCourse.items.isEmpty()

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseProjectGenerator::class.java)

    @JvmField
    val EDU_PROJECT_CREATED = Key.create<Boolean>("edu.projectCreated")
    @JvmField
    val COURSE_MODE_TO_CREATE = Key.create<CourseMode>("edu.courseModeToCreate")
    val COURSE_LANGUAGE_ID_TO_CREATE = Key.create<String>("edu.courseLanguageIdToCreate")


    // TODO: provide more precise heuristic for Gradle, sbt and other "dangerous" build systems
    // See https://youtrack.jetbrains.com/issue/EDU-4182
    private fun isCourseTrusted(course: Course, isNewCourseCreatorCourse: Boolean): Boolean {
      if (isNewCourseCreatorCourse) return true
      if (course !is EduCourse) return true
      if (course.visibility is FeaturedVisibility) return true
      return course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) == true
    }
  }
}
