package com.jetbrains.edu.cpp

import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduTestCase
import org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase
import org.junit.Assert.assertThat

class CppUtilTest : EduTestCase() {

  fun `test find CMake command - project`() = checkCMakeCommand("""
    |#some text
    |
    |project(name)
    |
    |# some text
  """, "project")

  fun `test find CMake command - ProJEct`() = checkCMakeCommand("""
    | #some text
    | 
    | ProJEct(name)
  """, "project")

  fun `test find CMake command - inline call`() = checkCMakeCommand("""
    | message("Hi!") project(name) message("By!")
  """, "project")

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
      assertThat(result.name, equalToIgnoringCase(commandName))
    }
    else {
      assertNull(result)
    }
  }
}
