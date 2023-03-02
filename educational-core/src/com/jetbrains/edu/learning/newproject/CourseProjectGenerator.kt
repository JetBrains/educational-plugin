package com.jetbrains.edu.learning.newproject

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.impl.TrustedPaths
import com.intellij.ide.util.PropertiesComponent
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.util.PathUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.isLocalCourse
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseVisibility.FeaturedVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ONLY_IDEA_DIRECTORY
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.unpackAdditionalFiles
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
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
) : PlatformCourseProjectGenerator() {
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
    // project.isLocalCourse info is stored in PropertiesComponent to keep it after course restart on purpose
    // not to show login widget for local course
    project.isLocalCourse = course.isLocal

    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateAllWidgets()

    setUpPluginDependencies(project, course)

    loadSolutions(project, course)
    EduUtils.openFirstTask(course, project)

    YamlFormatSynchronizer.saveAll(project)
    YamlFormatSynchronizer.startSynchronization(project)
    NOTIFICATIONS_SILENT_MODE.set(project, true)
  }

  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little differently
  // we use Object instead of S and cast to S when it needed
  fun doCreateCourseProject(location: String, projectSettings: Any): Project? {
    if (!beforeProjectGenerated()) {
      return null
    }
    val createdProject = createProject(location) ?: return null
    @Suppress("UNCHECKED_CAST")
    val castedProjectSettings = projectSettings as S
    afterProjectGenerated(createdProject, castedProjectSettings)
    return createdProject
  }

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use [CourseProjectGenerator.createCourseStructure]
   *
   * @param locationString location of new project
   *
   * @return project of new course or null if new project can't be created
   */
  private fun createProject(locationString: String): Project? {
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

    if (isCourseTrusted(course, isNewCourseCreatorCourse)) {
      @Suppress("UnstableApiUsage")
      TrustedPaths.getInstance().setProjectPathTrusted(location.toPath(), true)
    }

    val holder = CourseInfoHolder.fromCourse(course, baseDir)
    // @formatter:off
    ProgressManager.getInstance().runProcessWithProgressSynchronously<Unit, IOException>({
      createCourseStructure(holder)
    }, EduCoreBundle.message("generate.project.generate.course.structure.progress.text"), false, null)
    // @formatter:on

    val newProject = openNewCourseProject(course, location.toPath(), this::prepareToOpen) ?: return null

    // @formatter:off
    ProgressManager.getInstance().runProcessWithProgressSynchronously<Unit, IOException>({
      unpackAdditionalFiles(holder, ONLY_IDEA_DIRECTORY)
    }, EduCoreBundle.message("generate.project.unpack.course.project.settings.progress.text"), false, newProject)
    // @formatter:on

    // after adding files with settings to .idea directory, almost all settings are synchronized automatically,
    // but the inspection profiles are to be synchronized manually
    ProjectInspectionProfileManager.getInstance(newProject).initializeComponent()

    return newProject
  }

  /**
   * Creates course structure in directory provided by [holder]
   */
  @VisibleForTesting
  open fun createCourseStructure(holder: CourseInfoHolder<Course>) {
    holder.course.init(false)
    val isNewCourseCreatorCourse = isNewCourseCreatorCourse

    if (isNewCourseCreatorCourse) {
      val lesson = courseBuilder.createInitialLesson(holder)
      if (lesson != null) {
        course.addLesson(lesson)
      }
    }

    try {
      generateCourseContent(holder, isNewCourseCreatorCourse, ProgressManager.getInstance().progressIndicator)
    }
    catch (e: IOException) {
      LOG.error("Failed to generate course", e)
    }
  }

  @Throws(IOException::class)
  private fun generateCourseContent(
    holder: CourseInfoHolder<Course>,
    isNewCourseCreatorCourse: Boolean,
    indicator: ProgressIndicator
  ) {
    val duration = measureTimeMillis {
      val course = holder.course
      if (!course.isStudy) {
        CCUtils.initializeCCPlaceholders(holder)
      }
      GeneratorUtils.createCourse(holder, indicator)
      if (course is EduCourse && (course.isStepikRemote || course.isMarketplaceRemote) && !course.isStudy) {
        checkIfAvailableOnRemote(course)
      }
      createAdditionalFiles(holder, isNewCourseCreatorCourse)
      EduCounterUsageCollector.eduProjectCreated(course)
    }
    LOG.info("Course content generation: $duration ms")
  }

  private fun checkIfAvailableOnRemote(course: EduCourse) {
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
      course.convertToLocal()
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
   * @param holder contains info about course project like root directory
   * @param isNewCourse `true` if course is new one, `false` otherwise
   *
   * @throws IOException
   */
  @Throws(IOException::class)
  open fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {}

  private val isNewCourseCreatorCourse: Boolean
    get() = course.courseMode == CourseMode.EDUCATOR && course.items.isEmpty()

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseProjectGenerator::class.java)
    private const val JETBRAINS_SRO_ORGANIZATION: String = "JetBrains s.r.o."

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
      // Trust any marketplace course from JetBrains s.r.o. organization
      if (course.isMarketplaceRemote && course.organization == JETBRAINS_SRO_ORGANIZATION) return true
      return course.isPreview
    }
  }
}
