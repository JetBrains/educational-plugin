package com.jetbrains.edu.cpp

import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduTestCase
import org.hamcrest.text.IsEqualIgnoringCase
import org.junit.Assert.assertThat
import org.junit.Test

class CppUtilTest : EduTestCase() {

  @Test
  fun `test find CMake command - project`() = checkCMakeCommand("""
    |#some text
    |
    |project(name)
    |
    |# some text
  """, "project")

  @Test
  fun `test find CMake command - ProJEct`() = checkCMakeCommand("""
    | #some text
    | 
    | ProJEct(name)
  """, "project")

  @Test
  fun `test find CMake command - inline call`() = checkCMakeCommand("""
    | message("Hi!") project(name) message("By!")
  """, "project")

  @Test
  fun `test find CMake command - no command`() = checkCMakeCommand("""
    | #some text
    | message("empty me )=")
    | # no text ;)
  """, "project", shouldFind = false)

  private fun checkCMakeCommand(text: String, commandName: String, shouldFind: Boolean = true) {
    val file = myFixture.configureByText(CMakeListsFileType.FILE_NAME, text.trimMargin())
    val result = file.findCMakeCommand(commandName)
    if (shouldFind) {
      check(result != null)
      assertThat(result.name, IsEqualIgnoringCase(commandName))
    }
    else {
      assertNull(result)
    }
  }
}
