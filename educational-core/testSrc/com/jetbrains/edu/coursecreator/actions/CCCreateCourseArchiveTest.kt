package com.jetbrains.edu.coursecreator.actions

import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.externalDependencies.ProjectExternalDependency
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.coursecreator.actions.CCCreateCourseArchiveTest.PlainTextCompatibilityProvider.Companion.PLAIN_TEXT_PLUGIN_ID
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.exceptions.BrokenPlaceholderException
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.setUpPluginDependencies
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import com.jetbrains.edu.learning.yaml.configFileName
import java.text.SimpleDateFormat
import java.util.*

class CCCreateCourseArchiveTest : CourseArchiveTestBase() {

  fun `test local course archive`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test course ignore`() {
    val lessonIgnoredFile = "lesson1/LessonIgnoredFile.txt"
    val courseIgnoredFile = "IgnoredFile.txt"
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      additionalFile(lessonIgnoredFile)
      additionalFile(courseIgnoredFile)
      additionalFile(EduNames.COURSE_IGNORE, "$courseIgnoredFile\n${lessonIgnoredFile}\n\n")
    }
    course.description = "my summary"
    doTest()
  }

  fun `test local course with author`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    course.authors = listOf(StepikUserInfo("EduTools Dev"), StepikUserInfo("EduTools QA"),
                            StepikUserInfo("EduTools"))
    doTest()
  }

  fun `test course with specific json version`() {
    val jsonVersion = 10
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    } as EduCourse
    course.description = "my summary"
    course.formatVersion = jsonVersion
    doTest()
  }

  fun `test local course with content tags`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    course.contentTags = listOf("kotlin", "cycles")
    doTest()
  }

  fun `test framework lesson archive`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("my lesson") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test framework lesson with content tags`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("my lesson") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }
    course.description = "my summary"
    course.lessons[0].contentTags = listOf("kotlin", "cycles")
    doTest()
  }

  fun `test framework lesson with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("my lesson", customPresentableName = "custom name") {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test lesson with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson(customPresentableName = "custom name") {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test sections`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test section with content tags`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.description = "my summary"
    val section = course.sections[0]
    section.contentTags = listOf("kotlin", "cycles")
    doTest()
  }

  fun `test section with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section(customPresentableName = "custom name") {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test custom files`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
            taskFile("test.py", "some test")
            taskFile("additional.py", "my test", visible = false)
            taskFile("visibleAdditional.py", "my test")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test mp3 audio task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("test.mp3")
        }
      }
    }
    doTest()
  }

  fun `test mp4 video task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("test.mp4")
        }
      }
    }
    doTest()
  }

  fun `test png picture task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("123.png")
        }
      }
    }
    doTest()
  }

  fun `test pdf task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("123.pdf")
        }
      }
    }
    doTest()
  }

  fun `test git object task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("8abe7b618ddf9c55adbea359ce891775794a61")
        }
      }
    }
    doTest()
  }

  fun `test remote course archive`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }.asRemote()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val date = dateFormat.parse("Jan 01, 1970 03:00:00 AM")
    course.updateDate = date
    for (lesson in course.lessons) {
      lesson.updateDate = date
      for (task in lesson.taskList) {
        task.updateDate = date
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test placeholder dependencies`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """) {
            placeholder(0, dependency = "lesson1#task1#fizz.kt#1")
            placeholder(1, dependency = "lesson1#task1#fizz.kt#2")
          }
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test throw exception if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    course.description = "my summary"
    val placeholder = course.lessons.first().taskList.first().taskFiles["fizz.kt"]?.answerPlaceholders?.firstOrNull()
                      ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    assertThrows(BrokenPlaceholderException::class.java, {
      CourseArchiveCreator.loadActualTexts(project, course)
    })
  }

  fun `test navigate to yaml if placeholder is broken`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """fn fizzz() = <p>TODO()</p>""")
        }
      }
    }
    course.description = "my summary"
    createConfigFiles(project)

    val task = course.lessons.first().taskList.first()
    val placeholder = task.taskFiles["fizz.kt"]?.answerPlaceholders?.firstOrNull() ?: error("Cannot find placeholder")
    placeholder.offset = 1000

    assertNull(FileEditorManagerEx.getInstanceEx(project).currentFile)

    // It is not important, what would be passed to the constructor, except the first argument - project
    // Inside `compute()`, exception would be thrown, so we will not reach the moment of creating the archive
    getArchiveCreator().compute()

    val navigatedFile = FileEditorManagerEx.getInstanceEx(project).currentFile ?: error("Navigated file should not be null here")
    assertEquals(task.configFileName, navigatedFile.name)
  }

  fun `test course additional files`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
      }
      additionalFiles {
        eduFile("additional.txt", "file text")
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test course with choice tasks`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(isMultipleChoice = true, choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("task.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test course with choice with customized messages`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        choiceTask(
          isMultipleChoice = true,
          choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT),
          messageCorrect = "You are good!",
          messageIncorrect = "You are not good!",
          quizHeader = "Come on! You can do it!",
        ) {
          taskFile("task.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test course programming language ID and version`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("fizz.kt", """
          fn fizzz() = <p>TODO()</p>
          fn buzz() = <p>TODO()</p>
        """)
        }
      }
    }
    course.languageVersion = "11"
    doTest()
  }

  fun `test task with custom name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = "custom name") {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test task with content tags`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    val task = course.lessons.first().taskList.first()
    task.contentTags = listOf("kotlin", "cycles")
    doTest()
  }

  fun `test peek solution is hidden for course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.solutionsHidden = true
    course.description = "my summary"
    doTest()
  }

  fun `test peek solution is hidden for task`() {
    val task = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
    }.findTask("lesson1", "task1")
    task.solutionHidden = true
    doTest()
  }

  fun `test gradle properties additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("gradle.properties", "some.awesome.property=true")
    }
    doTest()
  }

  fun `test mp3 audio additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.mp3")
    }
    doTest()
  }

  fun `test mp4 video additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.mp4")
    }
    doTest()
  }

  fun `test png picture additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.png")
    }
    doTest()
  }

  // EDU-2765
  fun `test pdf additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.pdf")
    }
    doTest()
  }

  fun `test git object additional file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1")
      }
      additionalFile("8abe7b618ddf9c55adbea359ce891775794a61")
    }
    doTest()
  }

  fun `test non templated based framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson1", isTemplateBased = false) {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    doTest()
  }

  fun `test remote non templated based framework lesson`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, id = 1) {
      frameworkLesson("lesson1", isTemplateBased = false) {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }.apply {
      description = "my summary"
      updateDate = Date(86486865)
    }
    doTest()
  }

  fun `test local course with plugins`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    course.description = "my summary"
    ExternalDependenciesManager.getInstance(project).allDependencies = mutableListOf<ProjectExternalDependency>(
      DependencyOnPlugin("testPluginId", "1.0", null))

    try {
      doTest()
    }
    finally {
      ExternalDependenciesManager.getInstance(project).allDependencies = mutableListOf<ProjectExternalDependency>()
    }
  }

  fun `test local course with plugin from compatibility provider`() {
    val ep = CourseCompatibilityProviderEP()
    ep.language = PlainTextLanguage.INSTANCE.id
    ep.implementationClass = PlainTextCompatibilityProvider::class.java.name
    ep.pluginDescriptor = testPluginDescriptor
    CourseCompatibilityProviderEP.EP_NAME.point.registerExtension(ep, testRootDisposable)

    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary") {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    // Invoke it manually since `courseWithFiles` doesn't call it
    setUpPluginDependencies(project, course)
    val dependenciesManager = ExternalDependenciesManager.getInstance(project)
    val dependency = dependenciesManager.allDependencies.find { it is DependencyOnPlugin && it.pluginId == PLAIN_TEXT_PLUGIN_ID }
    assertNotNull("`${course.name}` course should have `$PLAIN_TEXT_PLUGIN_ID` plugin dependency", dependency)

    try {
      doTest()
    }
    finally {
      dependenciesManager.allDependencies = mutableListOf<ProjectExternalDependency>()
    }
  }

  fun `test custom command`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary") {
      lesson("lesson1") {
        theoryTask("TheoryWithCustomRunConfiguration") {
          taskFile("main.py", """
            import os

            if __name__ == "__main__":
                print(os.getenv("EXAMPLE_ENV"))
          """)
          // Need to verify that the plugin doesn't touch non-related run configuration files
          xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomCustomRun" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course7" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="${'$'}PROJECT_DIR${'$'}/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration/main.py" />
                  <option name="PARAMETERS" value="" />
                  <option name="SHOW_COMMAND_LINE" value="false" />
                  <option name="EMULATE_TERMINAL" value="false" />
                  <option name="MODULE_MODE" value="false" />
                  <option name="REDIRECT_INPUT" value="false" />
                  <option name="INPUT_FILE" value="" />
                  <method v="2" />
                </configuration>
              </component>
            """)
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="PythonConfigurationType" factoryName="Python">
                  <module name="Python Course7" />
                  <option name="INTERPRETER_OPTIONS" value="" />
                  <option name="PARENT_ENVS" value="true" />
                  <envs>
                    <env name="PYTHONUNBUFFERED" value="1" />
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="SDK_HOME" value="${'$'}PROJECT_DIR${'$'}/.idea/VirtualEnvironment/bin/python" />
                  <option name="WORKING_DIRECTORY" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration" />
                  <option name="IS_MODULE_SDK" value="true" />
                  <option name="ADD_CONTENT_ROOTS" value="true" />
                  <option name="ADD_SOURCE_ROOTS" value="true" />
                  <EXTENSION ID="PythonCoverageRunConfigurationExtension" runner="coverage.py" />
                  <option name="SCRIPT_NAME" value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration/main.py" />
                  <option name="PARAMETERS" value="" />
                  <option name="SHOW_COMMAND_LINE" value="false" />
                  <option name="EMULATE_TERMINAL" value="false" />
                  <option name="MODULE_MODE" value="false" />
                  <option name="REDIRECT_INPUT" value="false" />
                  <option name="INPUT_FILE" value="" />
                  <method v="2" />
                </configuration>
              </component>
            """)
          }
        }
      }
    }
    doTest()
  }

  fun `test only inspectionProfiles and scopes folders go into archive from the dot_idea folder`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary") {
      lesson("lesson1") {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      additionalFile(".idea/important_settings.xml", """
        some additional file that should not go into archive
      """.trimIndent())
      additionalFile(".idea/subfolder/important_settings_in_subfolder.xml", """
        some additional file that should not go into archive
      """.trimIndent())
      additionalFile(".idea/scopes/.dont include me.xml", """
        some hidden additional file that should not go into archive
      """.trimIndent())
      additionalFile(".idea/scopes/.dont include folder/x.xml", """
        some additional file inside a hidden folder that should not go into archive
      """.trimIndent())
      additionalFile(".idea/scopes/level_up.xml", """
        <component name="DependencyValidationManager">
          <scope name="level_up" pattern="file:lesson1/task3/*" />
        </component>
      """.trimIndent())
      additionalFile(".idea/inspectionProfiles/profiles_settings.xml", """
        <component name="InspectionProjectProfileManager">
          <settings>
            <option name="PROJECT_PROFILE" value="One more inspection profile" />
            <version value="1.0" />
          </settings>
        </component>
      """.trimIndent())
      additionalFile(".idea/inspectionProfiles/Project_Default.xml", """
        <component name="InspectionProjectProfileManager">
          <profile version="1.0">
            <option name="myName" value="Project Default" />
            <inspection_tool class="ReplaceAssignmentWithOperatorAssignment" enabled="true" level="INFORMATION" enabled_by_default="false">
              <scope name="level_up" level="WARNING" enabled="true" editorAttributes="WARNING_ATTRIBUTES">
                <option name="ignoreLazyOperators" value="true" />
                <option name="ignoreObscureOperators" value="false" />
              </scope>
              <option name="ignoreLazyOperators" value="true" />
              <option name="ignoreObscureOperators" value="false" />
            </inspection_tool>
            <inspection_tool class="unused" enabled="true" level="WARNING" enabled_by_default="false" checkParameterExcludingHierarchy="false">
              <scope name="level_up" level="WARNING" enabled="true" checkParameterExcludingHierarchy="false">
                <option name="LOCAL_VARIABLE" value="true" />
                <option name="FIELD" value="true" />
                <option name="METHOD" value="true" />
                <option name="CLASS" value="true" />
                <option name="PARAMETER" value="true" />
                <option name="REPORT_PARAMETER_FOR_PUBLIC_METHODS" value="true" />
                <option name="ADD_MAINS_TO_ENTRIES" value="true" />
                <option name="ADD_APPLET_TO_ENTRIES" value="true" />
                <option name="ADD_SERVLET_TO_ENTRIES" value="true" />
                <option name="ADD_NONJAVA_TO_ENTRIES" value="true" />
              </scope>
              <option name="LOCAL_VARIABLE" value="true" />
              <option name="FIELD" value="true" />
              <option name="METHOD" value="true" />
              <option name="CLASS" value="true" />
              <option name="PARAMETER" value="true" />
              <option name="REPORT_PARAMETER_FOR_PUBLIC_METHODS" value="true" />
              <option name="ADD_MAINS_TO_ENTRIES" value="true" />
              <option name="ADD_APPLET_TO_ENTRIES" value="true" />
              <option name="ADD_SERVLET_TO_ENTRIES" value="true" />
              <option name="ADD_NONJAVA_TO_ENTRIES" value="true" />
            </inspection_tool>
          </profile>
        </component>
      """.trimIndent())
      additionalFile(".idea/inspectionProfiles/One_more_inspections_profile.xml", """
        <component name="InspectionProjectProfileManager">
          <profile version="1.0">
            <option name="myName" value="One more inspection profile" />
            <inspection_tool class="AssignmentToNull" enabled="true" level="WARNING" enabled_by_default="true" />
            <inspection_tool class="IncrementDecrementUsedAsExpression" enabled="true" level="WARNING" enabled_by_default="true" />
            <inspection_tool class="ReplaceAssignmentWithOperatorAssignment" enabled="true" level="INFORMATION" enabled_by_default="false">
              <scope name="level_up" level="WARNING" enabled="true" editorAttributes="WARNING_ATTRIBUTES">
                <option name="ignoreLazyOperators" value="true" />
                <option name="ignoreObscureOperators" value="false" />
              </scope>
              <option name="ignoreLazyOperators" value="true" />
              <option name="ignoreObscureOperators" value="false" />
            </inspection_tool>
            <inspection_tool class="unused" enabled="true" level="WARNING" enabled_by_default="false" checkParameterExcludingHierarchy="false">
              <scope name="level_up" level="WARNING" enabled="true" editorAttributes="NOT_USED_ELEMENT_ATTRIBUTES" checkParameterExcludingHierarchy="false">
                <option name="LOCAL_VARIABLE" value="true" />
                <option name="FIELD" value="true" />
                <option name="METHOD" value="true" />
                <option name="CLASS" value="true" />
                <option name="PARAMETER" value="true" />
                <option name="REPORT_PARAMETER_FOR_PUBLIC_METHODS" value="true" />
                <option name="ADD_MAINS_TO_ENTRIES" value="true" />
                <option name="ADD_APPLET_TO_ENTRIES" value="true" />
                <option name="ADD_SERVLET_TO_ENTRIES" value="true" />
                <option name="ADD_NONJAVA_TO_ENTRIES" value="true" />
              </scope>
              <option name="LOCAL_VARIABLE" value="true" />
              <option name="FIELD" value="true" />
              <option name="METHOD" value="true" />
              <option name="CLASS" value="true" />
              <option name="PARAMETER" value="true" />
              <option name="REPORT_PARAMETER_FOR_PUBLIC_METHODS" value="true" />
              <option name="ADD_MAINS_TO_ENTRIES" value="true" />
              <option name="ADD_APPLET_TO_ENTRIES" value="true" />
              <option name="ADD_SERVLET_TO_ENTRIES" value="true" />
              <option name="ADD_NONJAVA_TO_ENTRIES" value="true" />
            </inspection_tool>
          </profile>
        </component>
      """.trimIndent())
    }
    doTest()
  }

  fun `test environment settings serialization`() {
    courseWithFiles {
      environmentSetting("example key 1", "example value 1")
      environmentSetting("example key 2", "example value 2")
    }
    doTest()
  }

  fun `test task file highlighting level serialization`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("a.java") {
            withHighlightLevel(EduFileErrorHighlightLevel.NONE)
          }
          taskFile("b.java") {
            withHighlightLevel(EduFileErrorHighlightLevel.ALL_PROBLEMS)
          }
          taskFile("c.java") {
            withHighlightLevel(EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION)
          }
          taskFile("d.java")
        }
      }
    }
    doTest()
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/createCourseArchive"
  }

  private class PlainTextCompatibilityProvider : CourseCompatibilityProvider {
    override val technologyName: String get() = "Plain Text"
    override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfo(PLAIN_TEXT_PLUGIN_ID))

    companion object {
      const val PLAIN_TEXT_PLUGIN_ID = "PlainText"
    }
  }
}
