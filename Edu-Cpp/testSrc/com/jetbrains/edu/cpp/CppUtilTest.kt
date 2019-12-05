package com.jetbrains.edu.cpp

import com.intellij.psi.PsiFileFactory
import com.jetbrains.cmake.CMakeLanguage
import com.jetbrains.edu.coursecreator.CCTestCase

class CppUtilTest : CCTestCase() {
  private fun `find CMake command base`(cMakeFile: String, commandName: String, shouldFind: Boolean = true) {
    val mockPsiFile = PsiFileFactory.getInstance(myFixture.project).createFileFromText(CMakeLanguage.INSTANCE, cMakeFile)
    val result = mockPsiFile.findCMakeCommand(commandName)
    if (shouldFind) {
      kotlin.test.assertNotNull(result)
      assert(result.name.equals(commandName, true))
    }
    else {
      assertNull(result)
    }
  }

  fun `test find CMake command - project`() =
    `find CMake command base`(
      """
        |#some text
        |
        |project(name)
        |
        |# some text
      """.trimMargin("|"),
      "project")

  fun `test find CMake command - ProJEct`() =
    `find CMake command base`(
      """
        | #some text
        | 
        | ProJEct(name)
      """.trimMargin("|"),
      "project"
    )

  fun `test find CMake command - inline call`() =
    `find CMake command base`(
      """
        | message("Hi!") project(name) message("By!")
      """.trimMargin("|"),
      "project"
    )

  fun `test find CMake command - no command`() =
    `find CMake command base`(
      """
        | #some text
        | message("empty me )=")
        | # no text ;)
      """.trimMargin("|"),
      "project",
      false
    )
}