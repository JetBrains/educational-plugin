package com.jetbrains.edu.learning.checker

import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.impl.ComponentManagerImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageManagerImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.UsefulTestCase
import com.intellij.ui.docking.DockContainer
import com.intellij.ui.docking.DockManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.createFileEditorManager
import org.junit.Assert
import org.junit.ComparisonFailure
import java.io.File

abstract class CheckersTestBase<Settings> : UsefulTestCase() {
    private lateinit var myManager: FileEditorManagerImpl
    private lateinit var myOldManager: FileEditorManager
    private lateinit var myOldDockContainers: Set<DockContainer>

    private lateinit var myCourse: Course
    protected lateinit var myProject: Project
    private lateinit var myApplication: IdeaTestApplication

    private lateinit var myTestDir: File

    private val MY_TEST_JDK_NAME = "Test JDK"

    override fun shouldRunTest(): Boolean {
        // We temporarily disable checkers tests on teamcity linux agents
        // because they don't work on these agents and we can't find out a reason :((
        return super.shouldRunTest() && (!SystemInfo.isLinux || System.getenv("TEAMCITY_VERSION") == null)
    }

    protected fun doTest() {
        UIUtil.dispatchAllInvocationEvents()

        val exceptions = arrayListOf<AssertionError>()
        for (lesson in myCourse.lessons) {
            for (task in lesson.taskList) {
                try {
                    val taskFile = task.taskFiles.values.first()
                    val virtualFile = taskFile.getVirtualFile(myProject)
                                      ?: error("Can't find virtual file for `${taskFile.name}` task file in `${task.name} task`")
                    FileEditorManager.getInstance(myProject).openFile(virtualFile, true)

                    launchAction(virtualFile, CheckAction())

                    UIUtil.dispatchAllInvocationEvents()
                } catch (e: AssertionError) {
                    exceptions.add(e)
                } catch (e: ComparisonFailure) {
                    exceptions.add(e)
                }
            }
        }
        if (exceptions.isNotEmpty()) {
            throw MultipleCauseException(exceptions)
        }
    }

    private class MultipleCauseException(val causes: List<AssertionError>) : Exception() {
        override fun printStackTrace() {
            for (cause in causes) {
                cause.printStackTrace()
            }
        }

        override val message: String?
            get() = "\n" + causes.joinToString("\n") { it.message ?: "" }
    }

    protected abstract val courseBuilder: EduCourseBuilder<Settings>
    protected abstract val projectSettings: Settings
    protected abstract fun createCourse(): Course

    private fun projectName() = getTestName(true)

    private fun launchAction(virtualFile: VirtualFile, action: AnAction) {
        val e = getActionEvent(virtualFile, action)
        action.beforeActionPerformedUpdate(e)
        Assert.assertTrue(e.presentation.isEnabled && e.presentation.isVisible)
        action.actionPerformed(e)
    }

    private fun getActionEvent(virtualFile: VirtualFile, action: AnAction): TestActionEvent {
        val context = MapDataContext()
        context.put(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
        context.put<Project>(CommonDataKeys.PROJECT, myProject)
        return TestActionEvent(context, action)
    }

    private fun createEduProject() {
        myCourse = createCourse()

        val settings = projectSettings

        val generator = courseBuilder.getCourseProjectGenerator(myCourse) ?: error("Failed to get `CourseProjectGenerator`")
        myProject = generator.doCreateCourseProject(myTestDir.absolutePath, settings as Any)
                    ?: error("Cannot create project with name ${projectName()}")
    }

    override fun setUp() {
        super.setUp()

        CheckActionListener.reset()

        myApplication = IdeaTestApplication.getInstance()

        setUpEnvironment()

        myTestDir = File(FileUtil.getTempDirectory())
        myTestDir.mkdirs()

        VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(myTestDir, true)!!)

        val prevDialog = Messages.setTestDialog(TestDialog.NO)
        try {
            createEduProject()
        } finally {
          Messages.setTestDialog(prevDialog)
        }

        InjectedLanguageManagerImpl.pushInjectors(myProject)

        val dockManager = DockManager.getInstance(myProject)
        myOldDockContainers = dockManager.containers
        myManager = createFileEditorManager(myProject)
        myOldManager = (myProject as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, myManager)
        (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
    }

    override fun tearDown() {
        try {
            FileUtilRt.delete(myTestDir)

            LightPlatformTestCase.doTearDown(myProject, myApplication)
            InjectedLanguageManagerImpl.checkInjectorsAreDisposed(myProject)

            tearDownEnvironment()

            DockManager.getInstance(myProject).containers
                    .filterNot { myOldDockContainers.contains(it) }
                    .forEach { Disposer.dispose(it) }

            (myProject as ComponentManagerImpl).registerComponentInstance(FileEditorManager::class.java, myOldManager)
            myManager.closeAllFiles()

            EditorHistoryManager.getInstance(myProject).files.forEach {
                EditorHistoryManager.getInstance(myProject).removeFile(it)
            }

            (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
            runWriteAction { Disposer.dispose(myProject) }
        } finally {
            super.tearDown()
        }
    }

    protected open fun setUpEnvironment() {}
    protected open fun tearDownEnvironment() {}
}
