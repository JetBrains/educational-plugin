package com.jetbrains.edu.learning.checker

import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.util.Disposer
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
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.createFileEditorManager
import com.jetbrains.edu.learning.registerComponent
import org.junit.Assert
import org.junit.ComparisonFailure
import java.io.File

abstract class CheckersTestBase<Settings> : UsefulTestCase() {
    private lateinit var myManager: FileEditorManagerImpl
    private lateinit var myOldDockContainers: Set<DockContainer>

    private lateinit var myCourse: Course
    protected lateinit var myProject: Project
    private lateinit var myApplication: IdeaTestApplication

    private lateinit var myTestDir: File

    private lateinit var checkerFixture: EduCheckerFixture<Settings>

    override fun runBare() {
        // Usually, fixture objects are initialized in `setUp` method.
        // But in our case, it's necessary to skip test if environment cannot be set up.
        // The most convenient place to locate the corresponding code is `EduCheckerFixture` itself.
        // So, `checkerFixture` should be initialized before `shouldRunTest`.
        // That's why it's created here
        checkerFixture = createCheckerFixture()
        super.runBare()
    }

    override fun shouldRunTest(): Boolean {
        val skipTestReason = checkerFixture.getSkipTestReason()
        return if (skipTestReason != null) {
            System.err.println("SKIP `$name`: $skipTestReason")
            false
        } else {
            super.shouldRunTest()
        }
    }

    protected open fun doTest() {
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

    protected abstract fun createCheckerFixture(): EduCheckerFixture<Settings>
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

        val settings = checkerFixture.projectSettings

        val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                         ?: error("Failed to get `CourseProjectGenerator`")
        myProject = generator.doCreateCourseProject(myTestDir.absolutePath, settings as Any)
                    ?: error("Cannot create project with name ${projectName()}")
    }

    override fun setUp() {
        super.setUp()
        myApplication = IdeaTestApplication.getInstance()
        myTestDir = File(FileUtil.getTempDirectory())
        myTestDir.mkdirs()

        VfsUtil.markDirtyAndRefresh(false, true, true, VfsUtil.findFileByIoFile(myTestDir, true)!!)

        checkerFixture.setUp()

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
        myProject.registerComponent(FileEditorManager::class.java, myManager, testRootDisposable)
        (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
        EduDocumentListener.setGlobalListener(myProject, testRootDisposable)

        CheckListener.EP_NAME.getPoint(null).registerExtension(CheckActionListener(), testRootDisposable)
        CheckActionListener.reset()
    }

    override fun tearDown() {
        try {
            checkerFixture.tearDown()
            FileUtilRt.delete(myTestDir)
            LightPlatformTestCase.doTearDown(myProject, myApplication)
            InjectedLanguageManagerImpl.checkInjectorsAreDisposed(myProject)

            DockManager.getInstance(myProject).containers
                    .filterNot { myOldDockContainers.contains(it) }
                    .forEach { Disposer.dispose(it) }

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
}
