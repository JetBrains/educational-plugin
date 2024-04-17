package com.jetbrains.edu.php.actions.move

import com.intellij.psi.PsiElement
import com.jetbrains.edu.learning.actions.move.MoveHandlerTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.php.lang.PhpLanguage
import org.junit.Test

class PhpMoveHandlerTest : MoveHandlerTestBase(PhpLanguage.INSTANCE) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.php") }
    doTest(findTarget) {
      phpTaskFile("foo.php", """
        <?php
          function functionName<caret>() {}
        ?> 
      """)
      phpTaskFile("bar.php")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.php") }
    doTest(findTarget) {
      phpTaskFile("foo.php", """
        <?php
          class Fruit<caret> {}
        ?> 
      """)
      phpTaskFile("bar.php")
    }
  }
}
