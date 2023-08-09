package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.utils.vfs.getFile
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.JBAccountUserInfo
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.marketplace.addVendor
import com.jetbrains.edu.learning.marketplace.api.Author
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.setMarketplaceAuthorsAsString
import com.jetbrains.edu.learning.marketplace.mockJBAccount
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.navigation.NavigationUtils.getFirstTask

class MarketplaceCourseArchiveTest : CourseArchiveTestBase() {

  fun `test user name as vendor`() {
    mockJBAccount()

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

    doTest()

    MarketplaceSettings.INSTANCE.setAccount(null)
  }

  fun `test course with author`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }.apply { isMarketplace = true }
    course.setMarketplaceAuthorsAsString(listOf(Author("EduTools Dev"), Author("EduTools QA"), Author("EduTools")))
    doTest()
  }

  fun `test vendor with email`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com" }
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }.apply { isMarketplace = true }

    doTest()
  }

  fun `test vendor with url`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; url = "jetbrains.com"}
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply { isMarketplace = true }

    doTest()
  }

  fun `test private course`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "new text")
    }.apply {
      isMarketplace = true
      isMarketplacePrivate = true
    }
    doTest()
  }

  fun `test plugin version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com"}
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "new text")
    }.apply { isMarketplace = true }

    doTest()
  }

  fun `test possible answer encrypted`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask("task1") {
          taskFile("Task.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
      }
    }.apply {
      isMarketplace = true
      description = "my summary"
    }
    doTest()
  }

  fun `test course version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
    }

    doTest()
  }

  fun `test course link`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }.apply {
      isMarketplace = true
      marketplaceCourseVersion = 5
      feedbackLink = "https://course_link.com"
    }

    doTest()
  }

  fun `test course programming language ID and version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    courseWithFiles(courseMode = CourseMode.EDUCATOR, language = FakeGradleBasedLanguage, courseVendor = vendor) {
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

    doTest()
  }

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
    doTest()
  }

  fun `test creating archive after changing task format`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {}
    val task = course.getLesson("lesson1")?.getTask("task1") ?: error("no task1 found")
    val taskDir = task.getDir(project.courseDir) ?: error("no dir for task1")

    // first, test that course archive is created with the task description in MD format

    val descriptionFile = taskDir.getFile("task.md")
    val mdDescription = "# MD description"

    runWriteAction {
      VfsUtil.saveText(descriptionFile, mdDescription)
    }

    val mdJson = generateJson()

    assertTrue("Json should contain correct description text", mdJson.contains(""""description_text" : "$mdDescription""""))
    assertTrue("Description format should be MD", mdJson.contains(""""description_format" : "MD""""))

    // second, test that if we rename the file from "task.md" to "task.html" outside IDE, the course archive is created in HTML format
    val htmlDescription = "<h1>HTML description</h1>"
    runWriteAction {
      descriptionFile.rename(null, "task.html")
      VfsUtil.saveText(descriptionFile, htmlDescription)
    }

    val htmlJson = generateJson()
    assertTrue("Json should contain correct description text", htmlJson.contains(""""description_text" : "$htmlDescription""""))
    assertTrue("Description format should be HTML", htmlJson.contains(""""description_format" : "HTML""""))
  }

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/marketplaceCourseArchive"
  }
}
