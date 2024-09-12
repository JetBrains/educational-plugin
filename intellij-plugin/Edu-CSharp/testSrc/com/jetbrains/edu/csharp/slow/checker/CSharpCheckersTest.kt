package com.jetbrains.edu.csharp.slow.checker

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TestDialog
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase.assertEquals
import com.intellij.testFramework.TestApplicationManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.csharp.CSharpCourseBuilder
import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.ui.getUICheckLabel
import com.jetbrains.rdclient.util.idea.toVirtualFile
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import com.jetbrains.rider.test.OpenSolutionParams
import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.BaseTestWithSolutionBase
import com.jetbrains.rider.test.env.enums.SdkVersion
import com.jetbrains.rider.test.scriptingApi.buildSolutionWithReSharperBuild
import com.jetbrains.rider.test.scriptingApi.waitForProjectModelReady
import org.junit.ComparisonFailure
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.time.Duration

@TestEnvironment(sdkVersion = SdkVersion.DOT_NET_8)
class CSharpCheckersTest : BaseTestWithSolutionBase() {

  // style preserved from Rider test classes
  private var myProject: Project? = null
  private lateinit var myCourse: Course
  private val project: Project
    get() = myProject!!

  private val openSolutionParams: OpenSolutionParams
    get() = OpenSolutionParams().apply {
      restoreNuGetPackages = true
      waitForCaches = true
      waitForSolutionBuilder = true
    }

  private fun doTest() {
    refreshProject()
    UIUtil.dispatchAllInvocationEvents()

    buildSolutionWithReSharperBuild(project, timeout = Duration.ofSeconds(60))
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

  private fun refreshProject() {
    myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    waitForProjectModelReady(project, Duration.ofSeconds(30))
  }

  @Test
  fun `task checker test`() {
    openCourseAndWait()
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> {
          CheckUtils.CONGRATULATIONS
        }

        else -> null
      }
    }
    doTest()
  }

  private fun checkTask(task: Task): List<AssertionError> {
    UIUtil.dispatchAllInvocationEvents()
    val exceptions = mutableListOf<AssertionError>()
    try {
      // In case of framework lessons, we can't just run checker for the given task
      // because code on file system may be not updated yet.
      // Launch `NextTaskAction` to make all necessary updates on file system in similar was as users do it
      val frameworkLesson = task.lesson as? FrameworkLesson
      val currentFrameworkLessonTask = frameworkLesson?.currentTask()
      if (frameworkLesson != null && currentFrameworkLessonTask != null && currentFrameworkLessonTask != task) {
        val file = currentFrameworkLessonTask.openFirstTaskFileInEditor()
        launchAction(file, getActionById(NextTaskAction.ACTION_ID))
        assertEquals(task, frameworkLesson.currentTask())
      }

      val virtualFile = task.openFirstTaskFileInEditor()
      launchAction(virtualFile, CheckAction(task.getUICheckLabel()))
      UIUtil.dispatchAllInvocationEvents()
    }
    catch (e: AssertionError) {
      exceptions.add(e)
    }
    catch (e: ComparisonFailure) {
      exceptions.add(e)
    }
    catch (e: IllegalStateException) {
      exceptions.add(AssertionError(e))
    }
    return exceptions
  }

  private fun launchAction(virtualFile: VirtualFile, action: AnAction) {
    val context = createDataEvent(virtualFile)
    testAction(action, context)
  }

  private fun createDataEvent(virtualFile: VirtualFile): DataContext {
    return SimpleDataContext.builder()
      .add(CommonDataKeys.VIRTUAL_FILE_ARRAY, arrayOf(virtualFile))
      .add(CommonDataKeys.PROJECT, myProject)
      .build()
  }

  private fun Task.openFirstTaskFileInEditor(): VirtualFile {
    val taskFile = taskFiles.values.first()
    val virtualFile = taskFile.getVirtualFile(project)
                      ?: error("Can't find virtual file for `${taskFile.name}` task file in `$name task`")

    FileEditorManager.getInstance(project).openFile(virtualFile, true)
    return virtualFile
  }

  private fun createCourse() = course("TestCourse", language = CSharpLanguage, courseMode = CourseMode.EDUCATOR) {
    additionalFile("TestCourse.sln", GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE))
    lesson {
      eduTask {
        dir("src") {
          taskFile(
            "Task.cs", """
          // ReSharper disable all CheckNamespace

          class Task
          {
              public static void Main(string[] args)
              {
                  // your code here
                  Console.WriteLine("Hello world");
              }
          }
        """
          )
        }
        dir("test") {
          taskFile(
            "Test.cs", """
          // ReSharper disable all CheckNamespace
          // Please ensure all the tests are contained within the same namespace
          
          [TestFixture]
          internal class Test
          {
              [Test]
              public void Test1()
              {
                  Assert.AreEqual(1, 1);
              }
          }
        """
          )
        }
        taskFile(
          "Lesson1.Task1.csproj", """
          <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFramework>net8.0</TargetFramework>
            <ImplicitUsings>enable</ImplicitUsings>
            <Nullable>enable</Nullable>
        
            <IsPackable>false</IsPackable>
            <IsTestProject>true</IsTestProject>
            <GenerateProgramFile>false</GenerateProgramFile>
          </PropertyGroup>
        
          <ItemGroup>
            <PackageReference Include="coverlet.collector" Version="6.0.0"/>
            <PackageReference Include="Microsoft.NET.Test.Sdk" Version="17.8.0"/>
            <PackageReference Include="NUnit" Version="3.14.0"/>
            <PackageReference Include="NUnit.Analyzers" Version="3.9.0"/>
            <PackageReference Include="NUnit3TestAdapter" Version="4.5.0"/>
          </ItemGroup>
        
          <ItemGroup>
            <Using Include="NUnit.Framework"/>
          </ItemGroup>
        
        </Project>
        """
        )
      }
    }
  }

  @BeforeMethod
  fun setUp() {
    if (myProject != null) {
      EduDocumentListener.setGlobalListener(myProject!!, disposable = this)
    }

    CheckActionListener.registerListener(this)
    CheckActionListener.reset()
  }

  @AfterMethod(alwaysRun = true)
  fun buildAndCloseProject() {
    closeSolutionIfOpened()
    TestApplicationManager.getInstanceIfCreated()?.setDataProvider(null)
  }

  private fun createCourseStructure(rootDir: VirtualFile): Project {
    myCourse = createCourse()
    val settings = CSharpProjectSettings()

    withTestDialog(TestDialog.NO) {
      val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                      ?: error("Failed to get `CourseProjectGenerator`")
      return generator.doCreateCourseProject(rootDir.path, settings)
             ?: error("Cannot create project with name ${myCourse.name}")
    }
    error("Project was not created")
  }

  private fun openCourseAndWait(): VirtualFile {
    val rootDir = tempTestDirectory.toVirtualFile(true) ?: error("$tempTestDirectory directory not found")
    myProject = createCourseStructure(rootDir)
    waitForSolution(project, openSolutionParams)
    return rootDir
  }

  private fun closeSolutionIfOpened() {
    if (myProject != null) {
      closeSolutionAndResetSettings(project)
      myProject = null
    }
  }

  private class MultipleCauseException(val causes: List<AssertionError>) : Exception() {
    override fun printStackTrace() {
      for (cause in causes) {
        cause.printStackTrace()
      }
    }

    override val message: String
      get() = "\n" + causes.joinToString("\n") { it.message ?: "" }
  }
}

