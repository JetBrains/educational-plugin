package com.jetbrains.edu.learning.format

import com.jetbrains.edu.learning.CourseMode
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.createCourseFromJson
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo

class UnmodifiableFormatTest : EduTestCase() {
  fun testCourseAuthors() {
    assertThrows(UnsupportedOperationException::class.java) { courseFromJson.authors.add(MarketplaceUserInfo("test")) }
  }

  private val courseFromJson: Course
    get() {
      return createCourseFromJson(testDataPath + testFile, CourseMode.STUDENT)
    }

  override fun getTestDataPath(): String = "${super.getTestDataPath()}/unmodifiableFormat/"

  private val testFile: String get() = "${getTestName(true)}.json"

}