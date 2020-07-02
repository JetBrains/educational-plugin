package com.jetbrains.edu.learning.checker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.EditorHistoryManager
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl
import com.intellij.openapi.fileEditor.impl.FileEditorProviderManagerImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.TestActionEvent
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.createFileEditorManager
import com.jetbrains.edu.learning.registerComponent
import org.junit.Assert
import org.junit.ComparisonFailure

abstract class CheckersTestBase<Settings> : HeavyPlatformTestCase() {
    private lateinit var myManager: FileEditorManagerImpl

    protected lateinit var myCourse: Course

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
        refreshProject()
        UIUtil.dispatchAllInvocationEvents()

        val exceptions = arrayListOf<AssertionError>()
        myCourse.visitLessons { lesson ->
            for (task in lesson.taskList) {
                exceptions += checkTask(task)
            }
        }

        if (exceptions.isNotEmpty()) {
            throw MultipleCauseException(exceptions)
        }
    }

    protected fun refreshProject() {
        myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    protected open fun checkTask(currentTask: Task): List<AssertionError> {
        UIUtil.dispatchAllInvocationEvents()
        val exceptions = mutableListOf<AssertionError>()
        try {
            val taskFile = currentTask.taskFiles.values.first()
            val virtualFile = taskFile.getVirtualFile(myProject)
                              ?: error("Can't find virtual file for `${taskFile.name}` task file in `${currentTask.name} task`")
            FileEditorManager.getInstance(myProject).openFile(virtualFile, true)
            launchAction(virtualFile, CheckAction())
            UIUtil.dispatchAllInvocationEvents()
        } catch (e: AssertionError) {
            exceptions.add(e)
        } catch (e: ComparisonFailure) {
            exceptions.add(e)
        }
        return exceptions
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

    override fun setUpProject() {
        checkerFixture.setUp()

        myCourse = createCourse()
        val settings = checkerFixture.projectSettings

        val prevDialog = Messages.setTestDialog(TestDialog.NO)
        try {
            val rootDir = tempDir.createTempDir()
            val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                            ?: error("Failed to get `CourseProjectGenerator`")
            myProject = generator.doCreateCourseProject(rootDir.absolutePath, settings as Any)
                        ?: error("Cannot create project with name ${projectName()}")

        } finally {
            Messages.setTestDialog(prevDialog)
        }
    }

    override fun setUp() {
        super.setUp()

        myManager = createFileEditorManager(myProject)
        myProject.registerComponent(FileEditorManager::class.java, myManager, testRootDisposable)
        (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
        EduDocumentListener.setGlobalListener(myProject, testRootDisposable)

        CheckActionListener.registerListener(testRootDisposable)
        CheckActionListener.reset()
    }

    override fun tearDown() {
        try {
            checkerFixture.tearDown()

            myManager.closeAllFiles()

            val editorHistoryManager = EditorHistoryManager.getInstance(myProject)
            editorHistoryManager.files.forEach {
                editorHistoryManager.removeFile(it)
            }

            (FileEditorProviderManager.getInstance() as FileEditorProviderManagerImpl).clearSelectedProviders()
        } finally {
            super.tearDown()
        }
    }
}
