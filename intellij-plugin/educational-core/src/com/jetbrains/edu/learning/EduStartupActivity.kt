package com.jetbrains.edu.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.SynchronizeTaskDescription
import com.jetbrains.edu.coursecreator.courseignore.CourseIgnoreFileType
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.EduNames.COURSE_IGNORE
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.EduUtilsKt.isNewlyCreated
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.handlers.UserCreatedFileListener
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.navigation.NavigationUtils.setHighlightLevelForFilesInTask
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.projectView.CourseViewPane
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting
import training.dsl.LessonContext
import training.dsl.highlightButtonById
import training.lang.AbstractLangSupport
import training.lang.LangManager
import training.learn.CourseManager
import training.learn.course.IftModule
import training.learn.course.KLesson
import training.learn.course.LearningCourseBase
import training.learn.course.LessonType
import training.statistic.LessonStartingWay
import training.ui.LEARN_TOOL_WINDOW_ID

class EduStartupActivity : StartupActivity.DumbAware {

  private val YAML_MIGRATED = "Edu.Yaml.Migrate"

  override fun runActivity(project: Project) {
    if (!project.isEduProject()) return

    val manager = StudyTaskManager.getInstance(project)
    val connection = ApplicationManager.getApplication().messageBus.connect(manager)
    if (!isUnitTestMode) {
      val vfsListener = if (project.isStudentProject()) UserCreatedFileListener(project) else CCVirtualFileListener(project, manager)
      connection.subscribe(VirtualFileManager.VFS_CHANGES, vfsListener)

      if (CCUtils.isCourseCreator(project)) {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(SynchronizeTaskDescription(project), manager)
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(CourseIgnoreDocumentListener(project), manager)
      }
      EduDocumentListener.setGlobalListener(project, manager)
    }

    ensureCourseIgnoreHasNoCustomAssociation()

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(LEARN_TOOL_WINDOW_ID)
      toolWindow!!.show()
      LangManager.getInstance().updateLangSupport("Python")
      CourseManager.instance.modules
      CourseManager.instance.openLesson(project, TestLesson(), LessonStartingWay.RESTORE_LINK, forceStartLesson = true)
      val course = manager.course
      if (course == null) {
        LOG.warn("Opened project is with null course")
        return@runWhenProjectIsInitialized
      }

      val fileEditorManager = FileEditorManager.getInstance(project)
      if (!fileEditorManager.hasOpenFiles()) {
        NavigationUtils.openFirstTask(course, project)
      }
      selectProjectView(project, true)

      migrateYaml(project, course)

      setupProject(project, course)
      val coursesStorage = CoursesStorage.getInstance()
      val location = project.basePath
      if (!coursesStorage.hasCourse(course) && location != null && !course.isPreview) {
        coursesStorage.addCourse(course, location)
      }

      SyncChangesStateManager.getInstance(project).updateSyncChangesState(course)

      runWriteAction {
        if (project.isStudentProject()) {
          course.visitTasks {
            setHighlightLevelForFilesInTask(it, project)
          }
        }

        EduCounterUsageCollector.eduProjectOpened(course)
      }
    }
  }

  @VisibleForTesting
  fun migrateYaml(project: Project, course: Course) {
    migratePropagatableYamlFields(project, course)
    migrateCanCheckLocallyYaml(project, course)
    YamlFormatSynchronizer.saveAll(project)
  }

  private fun migrateCanCheckLocallyYaml(project: Project, course: Course) {
    val propertyComponent = PropertiesComponent.getInstance(project)
    if (propertyComponent.getBoolean(YAML_MIGRATED)) return
    propertyComponent.setValue(YAML_MIGRATED, true)
    if (course !is HyperskillCourse && course !is StepikCourse) return

    course.visitTasks {
      if (it is ChoiceTask) {
        it.canCheckLocally = false
      }
    }
  }

  private fun migratePropagatableYamlFields(project: Project, course: Course) {
    if (!CCUtils.isCourseCreator(project)) return
    val propertiesComponent = PropertiesComponent.getInstance(project)
    if (propertiesComponent.getBoolean(YAML_MIGRATED_PROPAGATABLE)) return
    propertiesComponent.setValue(YAML_MIGRATED_PROPAGATABLE, true)

    var hasPropagatableFlag = false
    val nonPropagatableFiles = mutableListOf<TaskFile>()
    course.visitTasks { task: Task ->
      if (task.lesson is FrameworkLesson) {
        for (taskFile in task.taskFiles.values) {
          if (!taskFile.isPropagatable) {
            hasPropagatableFlag = true
            return@visitTasks
          }
          if (!taskFile.isVisible || !taskFile.isEditable) {
            nonPropagatableFiles += taskFile
          }
        }
      }
    }
    if (hasPropagatableFlag) return

    for (taskFile in nonPropagatableFiles) {
      taskFile.isPropagatable = false
    }
  }

  private fun ensureCourseIgnoreHasNoCustomAssociation() {
    runInEdt {
      runWriteAction {
        FileTypeManager.getInstance().associate(CourseIgnoreFileType, ExactFileNameMatcher(COURSE_IGNORE))
      }
    }
  }

  // In general, it's hack to select proper Project View pane for course projects
  // Should be replaced with proper API
  private fun selectProjectView(project: Project, retry: Boolean) {
    ToolWindowManager.getInstance(project).invokeLater(Runnable {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW)
      // Since 2020.1 project view tool window can be uninitialized here yet
      if (toolWindow == null) {
        if (retry) {
          selectProjectView(project, false)
        }
        else {
          LOG.warn("Failed to show Course View because Project View is not initialized yet")
        }
        return@Runnable
      }
      val projectView = ProjectView.getInstance(project)
      if (projectView != null) {
        val selectedViewId = ProjectView.getInstance(project).currentViewId
        if (CourseViewPane.ID != selectedViewId) {
          projectView.changeView(CourseViewPane.ID)
        }
      }
      else {
        LOG.warn("Failed to select Project View")
      }
      toolWindow.show()
    })
  }

  private fun setupProject(project: Project, course: Course) {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.warn("Failed to refresh gradle project: configurator for `${course.languageId}` is null")
      return
    }

    if (!isUnitTestMode && project.isNewlyCreated()) {
      configurator.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    // Android Studio creates `gradlew` not via VFS, so we have to refresh project dir
    runInBackground(project, EduCoreBundle.message("refresh.course.project.directory"), false) {
      VfsUtil.markDirtyAndRefresh(false, true, true, project.courseDir)
    }
  }

  companion object {
    @VisibleForTesting
    const val YAML_MIGRATED_PROPAGATABLE = "Edu.Yaml.Migrate.Propagatable"

    private val LOG = Logger.getInstance(EduStartupActivity::class.java)
  }
}



class TestLesson : KLesson("dasd", "dasd") {
  override val lessonContent: LessonContext.() -> Unit = {
    highlightButtonById("Run", clearHighlights = false)
  }
}

class TestLangSupport : AbstractLangSupport() {
  override val primaryLanguage: String
    get() = "Python"

  override fun applyToProjectAfterConfigure(): (Project) -> Unit = { }
  override fun checkSdk(sdk: Sdk?, project: Project) {

  }

  override fun isSdkConfigured(project: Project): Boolean {
    return true
  }
}

class TestModule(@NonNls id: String, @Nls name: String, @Nls description: String,
                 @NlsSafe val lessonsDir: String,
                 initLessons: () -> List<KLesson>)
  : IftModule(id, name, description, TestLangSupport(), LessonType.SCRATCH, initLessons) {
  override val sampleFilePath: String = "Task.txt"
  override fun preferredLearnWindowAnchor(project: Project): ToolWindowAnchor {
    return ToolWindowAnchor.LEFT
  }
}


class TestCourse : LearningCourseBase("Python") {
  override fun modules(): Collection<IftModule> {

    return listOf(TestModule(
      id = "edu.Onboarding",
      name = "ASDASD",
      description = "ASDASD",
      lessonsDir = "Onboarding",
    ) {
      listOf(
        TestLesson()
      )
    })
  }
}

