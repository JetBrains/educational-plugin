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
  protected val courseBuilder: EduCourseBuilder<S>,
  protected var course: Course
) {
  private var alreadyEnrolled = false

  open fun beforeProjectGenerated(): Boolean {
    if (course !is EduCourse || !course.isStepikRemote) return true
    val remoteCourse = course as EduCourse

    if (remoteCourse.id <= 0) return true
    return ProgressManager.getInstance().runProcessWithProgressSynchronously<Boolean, RuntimeException>(
      {
        ProgressManager.getInstance().progressIndicator.isIndeterminate = true
        if (EduSettings.getInstance().user != null) {
          alreadyEnrolled = StepikConnector.getInstance().isEnrolledToCourse(remoteCourse.id)
          if (!alreadyEnrolled) {
            StepikConnector.getInstance().enrollToCourse(remoteCourse.id)
          }
        }
        loadCourseStructure(remoteCourse)
        course = remoteCourse
        true
      }, EduCoreBundle.message("generate.project.loading.course.progress.text"), true, null)
  }

  open fun afterProjectGenerated(project: Project, projectSettings: S) {
    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateAllWidgets()

    setUpPluginDependencies(project, course)

    loadSolutions(project, course)
    EduUtils.openFirstTask(course, project)

    YamlFormatSynchronizer.saveAll(project)
    YamlFormatSynchronizer.startSynchronization(project)
  }

  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little differently
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

    baseDir.putUserData(COURSE_MODE_TO_CREATE, course.courseMode)
    baseDir.putUserData(COURSE_LANGUAGE_ID_TO_CREATE, course.languageID)

    if (isNewTrustedProjectApiAvailable && isCourseTrusted(course, isNewCourseCreatorCourse)) {
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
    GeneratorUtils.initializeCourse(project, course)
    val isNewCourseCreatorCourse = isNewCourseCreatorCourse

    // BACKCOMPAT: 2021.3. Drop it because project is marked as trusted in `createProject`
    if (!isNewTrustedProjectApiAvailable && isCourseTrusted(course, isNewCourseCreatorCourse)) {
      @Suppress("UnstableApiUsage")
      project.setTrusted(true)
    }
    if (isNewCourseCreatorCourse) {
      val lesson = courseBuilder.createInitialLesson(project, course)
      if (lesson != null) {
        course.addLesson(lesson)
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
        CCUtils.initializeCCPlaceholders(project, course)
      }
      GeneratorUtils.createCourse(project, course, baseDir, indicator)
      if (course is EduCourse &&
          (course.isStepikRemote || (course as EduCourse).isMarketplaceRemote) &&
          CCUtils.isCourseCreator(project)) {
        checkIfAvailableOnRemote()
      }
      createAdditionalFiles(project, baseDir, isNewCourseCreatorCourse)
      EduCounterUsageCollector.eduProjectCreated(course)
    }
    LOG.info("Course content generation: $duration ms")
  }

  private fun checkIfAvailableOnRemote() {
    val remoteCourse = if (course.isMarketplace) {
      MarketplaceConnector.getInstance().searchCourse(course.id, course.isMarketplacePrivate)
    }
    else {
      StepikConnector.getInstance().getCourseInfo(course.id, null, true)
    }
    if (remoteCourse == null) {
      val platformName = if (course.isMarketplace) MARKETPLACE else StepikNames.STEPIK
      LOG.warn("Failed to get $platformName course for imported from zip course with id: ${course.id}")
      LOG.info("Converting course to local. Course id: ${course.id}")
      (course as EduCourse).convertToLocal()
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
    get() = course.courseMode == CourseMode.EDUCATOR && course.items.isEmpty()

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
      return course.isPreview
    }
  }
}
