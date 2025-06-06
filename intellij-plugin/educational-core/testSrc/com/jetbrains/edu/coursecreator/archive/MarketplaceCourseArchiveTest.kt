package com.jetbrains.edu.coursecreator.archive

import com.intellij.ide.DataManager
import com.intellij.notification.Notification
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.writeText
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.IJSwingUtilities
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createTextChildFile
import com.jetbrains.edu.learning.marketplace.StudyItemIdGenerator
import com.jetbrains.edu.learning.marketplace.addVendor
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils.getFirstTask
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName
import io.mockk.every
import org.junit.Test

class MarketplaceCourseArchiveTest : CourseArchiveTestBase() {

  override fun setUp() {
    super.setUp()
    val mockGenerator = mockService<StudyItemIdGenerator>(project)
    val ids = generateSequence(1, Int::inc).iterator()
    every { mockGenerator.generateNewId() } answers { ids.next() }
    mockJBAccount(testRootDisposable)
  }

  @Test
  fun `test user name as vendor`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }.apply { isMarketplace = true }
    val account = MarketplaceAccount()
    account.userInfo = JBAccountUserInfo("Zinaida Smirnova")
    MarketplaceSettings.INSTANCE.setAccount(account)
    course.addVendor()

    doTest(course)

    MarketplaceSettings.INSTANCE.setAccount(null)
  }

  @Test
  fun `test vendor with email`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }.apply { isMarketplace = true }

    doTest(course)
  }

  @Test
  fun `test vendor with url`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; url = "jetbrains.com"}
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply { isMarketplace = true }

    doTest(course)
  }

  @Test
  fun `test private course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "new text")
    }.apply {
      isMarketplace = true
      isMarketplacePrivate = true
    }
    doTest(course)
  }

  @Test
  fun `test plugin version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com"}
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "new text")
    }.apply { isMarketplace = true }

    doTest(course)
  }

  @Test
  fun `test course version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
    }

    doTest(course)
  }

  @Test
  fun `test course link`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
      feedbackLink = "https://course_link.com"
    }

    doTest(course)
  }

  @Test
  fun `test course programming language ID and version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
      feedbackLink = "https://course_link.com"
      languageVersion = "11"
    }

    doTest(course)
  }

  @Test
  fun `test task feedback link`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
    }
    val firstTask = getFirstTask(course) ?: return
    firstTask.feedbackLink = "https://task_link.com"
    doTest(course)
  }

  @Test
  fun `test creating archive after changing task format`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {}
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("no task1 found")
    val taskDir = task.getDir(project.courseDir) ?: error("no dir for task1")

    // first, test that course archive is created with the task description in MD format

    val descriptionFile = taskDir.findChild("task.md") ?: error("no task.md file found")
    val mdDescription = "# MD description"

    runWriteAction {
      VfsUtil.saveText(descriptionFile, mdDescription)
    }

    val mdJson = createCourseArchiveAndCheck(course).courseJson

    assertTrue("Json should contain correct description text", mdJson.contains(""""description_text" : "$mdDescription""""))
    assertTrue("Description format should be MD", mdJson.contains(""""description_format" : "MD""""))

    // second, test that if we rename the file from "task.md" to "task.html" outside IDE, the course archive is created in HTML format
    val htmlDescription = "<h1>HTML description</h1>"
    runWriteAction {
      descriptionFile.rename(null, "task.html")
      VfsUtil.saveText(descriptionFile, htmlDescription)
    }

    val htmlJson = createCourseArchiveAndCheck(course).courseJson
    assertTrue("Json should contain correct description text", htmlJson.contains(""""description_text" : "$htmlDescription""""))
    assertTrue("Description format should be HTML", htmlJson.contains(""""description_format" : "HTML""""))
  }

  @Test
  fun `test change remote info files course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson {
        eduTask("task1") {
          taskFile("Task.txt")
        }
      }
    }.asRemote().apply { isMarketplace = true }
    createConfigFiles(project)

    // Change `*-remote-info.yaml`s to ensure it will not equal to the original one
    runWriteAction {
      findFile("lesson1/task1/${REMOTE_TASK_CONFIG}").writeText("id: 11")
      findFile("lesson1/${REMOTE_LESSON_CONFIG}").writeText("id: 12")
      FileDocumentManager.getInstance().saveAllDocuments()
    }

    doTest(course)
  }

  @Test
  fun `test duplicate ids in course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary") {
      lesson {
        eduTask("task1", stepId = 1) {
          taskFile("Task.kt")
        }
        eduTask("task2", stepId = 1) {
          taskFile("Task.kt")
        }
      }
    }

    createCourseArchiveWithError<DuplicateIdsError>(course)
  }

  @Test
  fun `test duplicate ids notification action`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary", createYamlConfigs = true) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("Task.txt")
        }
        eduTask("task2", stepId = 1) {
          taskFile("Task.txt")
        }
      }
    }

    val duplicateIdsError = createCourseArchiveWithError<DuplicateIdsError>(course)

    val notification = duplicateIdsError.notification(project, "test title")

    val action = notification.actions.single {
      // Simple additional check not to get the wrong action in the future
      it.templatePresentation.text == EduCoreBundle.message("action.Educational.Educator.RegenerateDuplicateIds.text")
    }

    val context = SimpleDataContext.builder()
      .setParent(DataManager.getInstance().getDataContext(null))
      .add(Notification.KEY, notification)
      .build()

    testAction(action, context)
    // We know that this particular action should expire the notification,
    // so here we use this knowledge as a signal that the action finished.
    PlatformTestUtil.waitWhileBusy { !notification.isExpired }

    assertEquals(2, course.findTask("lesson1", "task1").id)
    assertEquals(3, course.findTask("lesson1", "task2").id)
  }

  @Test
  fun `test duplicate ids notification links`() {
    // given
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary", createYamlConfigs = true) {
      lesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("Task.txt")
        }
        eduTask("task2", stepId = 1) {
          taskFile("Task.txt")
        }
      }
    }

    // when
    val duplicateIdsError = createCourseArchiveWithError<DuplicateIdsError>(course)
    val notification = duplicateIdsError.notification(project, "test title")
    val listener = notification.listener ?: error("Failed to get notification listener")
    val link = HREF_LINK_REGEX.find(notification.content)?.groups?.get("link")?.value
               ?: error("Failed to find a link in the notification text")
    listener.hyperlinkUpdate(notification, IJSwingUtilities.createHyperlinkEvent(link, notification))

    // then
    val currentFile = FileEditorManagerEx.getInstanceEx(project).currentFile
    val expectedOpenFile = findFile("lesson1/task1/$REMOTE_TASK_CONFIG")
    assertEquals(expectedOpenFile, currentFile)
  }

  @Test
  fun `broken remote config for course does not allow creating the course archive`() =
    testBrokenRemoteConfig(".")

  @Test
  fun `broken remote config for section does not allow creating the course archive`() =
    testBrokenRemoteConfig("section1")

  @Test
  fun `broken remote config for lesson does not allow creating the course archive`() =
    testBrokenRemoteConfig("lesson2")

  @Test
  fun `broken remote config for lesson in section does not allow creating the course archive`() =
    testBrokenRemoteConfig("section1/lesson1")

  @Test
  fun `broken remote config for task does not allow creating the course archive`() =
    testBrokenRemoteConfig("section1/lesson1/task1")

  private fun testBrokenRemoteConfig(itemPath: String, configText: String = "{92*#&$*&@#%") {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "my summary", createYamlConfigs = true) {
      section("section1") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      lesson("lesson2") {
        eduTask("task2")
      }
    }.apply { isMarketplace = true }
    val itemDir = project.courseDir.findFileByRelativePath(itemPath) ?: error("Failed to find $itemPath")
    val item = itemDir.getStudyItem(project) ?: error("Failed to get StudyItem for $itemPath")

    val brokenConfigFile = createTextChildFile(project, itemDir, item.remoteConfigFileName, configText)
      ?: error("failed to create file ${item.remoteConfigFileName}")

    createCourseArchiveWithError<BrokenRemoteYamlError>(course)

    runWriteActionAndWait {
      // broken config does not allow course loading in tearDown()
      brokenConfigFile.delete(this)
    }
  }

  override fun getTestDataPath(): String {
    return "$TEST_DATA_ROOT/archive/marketplaceCourseArchive"
  }

  companion object {
    private val HREF_LINK_REGEX = """<a href='(?<link>.*?)'>""".toRegex()
  }
}
