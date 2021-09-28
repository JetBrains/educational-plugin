package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.marketplace.MARKETPLACE_LISTED_COURSES_LINK
import com.jetbrains.edu.learning.stepik.STEPIK_LISTED_COURSES_LINK

class ListedCoursesLoaderTest : EduTestCase()  {

  fun `test marketplace listed courses link`() {
    assertEquals("https://raw.githubusercontent.com/JetBrains/educational-plugin/master/marketplaceListedCourses.txt", MARKETPLACE_LISTED_COURSES_LINK)
  }

  fun `test stepik listed courses link`() {
    assertEquals("https://raw.githubusercontent.com/JetBrains/educational-plugin/master/listedCourses.txt", STEPIK_LISTED_COURSES_LINK)
  }
}