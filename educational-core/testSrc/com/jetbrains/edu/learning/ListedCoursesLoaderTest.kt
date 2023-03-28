package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.marketplace.MARKETPLACE_LISTED_COURSES_LINK

class ListedCoursesLoaderTest : EduTestCase()  {

  fun `test marketplace listed courses link`() {
    assertEquals("https://raw.githubusercontent.com/JetBrains/educational-plugin/master/marketplaceListedCourses.txt", MARKETPLACE_LISTED_COURSES_LINK)
  }
}