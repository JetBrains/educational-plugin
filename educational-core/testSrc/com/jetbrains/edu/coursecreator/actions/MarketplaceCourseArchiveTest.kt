package com.jetbrains.edu.coursecreator.actions

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCUtils.GENERATED_FILES_FOLDER
import com.jetbrains.edu.coursecreator.actions.marketplace.MarketplaceArchiveCreator
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.Vendor
import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo
import com.jetbrains.edu.learning.marketplace.settings.MarketplaceSettings
import com.jetbrains.edu.learning.setMarketplaceAuthorsAsString

class MarketplaceCourseArchiveTest : CourseArchiveTestBase() {

  fun `test user name as vendor`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }
    val account = MarketplaceAccount()
    account.userInfo = MarketplaceUserInfo("User Name")
    MarketplaceSettings.INSTANCE.account = account

    val creator = getArchiveCreator()
    creator.addVendor(course)

    doTest()

    MarketplaceSettings.INSTANCE.account = null
  }

  fun `test course with author`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o" }
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }
    course.setMarketplaceAuthorsAsString(listOf("EduTools Dev", "EduTools QA", "EduTools"))
    doTest()
  }

  fun `test vendor with email`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com" }
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "some text")
    }

    doTest()
  }

  fun `test vendor with url`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; url = "jetbrains.com"}
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "another text")
    }

    doTest()
  }

  fun `test plugin version`() {
    val vendor = Vendor().apply { name = "Jetbrains s.r.o"; email = "academy@jetbrains.com"}
    courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = FakeGradleBasedLanguage, courseVendor = vendor) {
      lesson("lesson1") {
        eduTask("task1") {}
      }
      additionalFile("test.txt", "new text")
    }

    doTest()
  }

  fun `test possible answer encrypted`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
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

  override fun getTestDataPath(): String {
    return super.getTestDataPath() + "/actions/marketplaceCourseArchive"
  }

  override fun getArchiveCreator() =
    MarketplaceArchiveCreator(myFixture.project, "${myFixture.project.basePath}/$GENERATED_FILES_FOLDER/course.zip")

}
