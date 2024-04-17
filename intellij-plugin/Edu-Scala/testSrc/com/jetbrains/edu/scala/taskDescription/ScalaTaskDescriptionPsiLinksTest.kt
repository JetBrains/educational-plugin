package com.jetbrains.edu.scala.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionPsiLinksTestBase
import org.jetbrains.plugins.scala.ScalaFileType
import org.junit.Test

class ScalaTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = ScalaFileType.INSTANCE

  @Test
  fun `test navigate to class`() = doTest("psi_element://Bar", """
    class <caret>Bar(val property: Int) {
      def bar(): Unit = {}
    }
  """) {
    scala("Foo.scala", """
      class Foo(val property: Int) {
        def foo(): Unit = {}
      }
    """)
    scala("Bar.scala", """
      class Bar(val property: Int) {
        def bar(): Unit = {}
      }
    """)
  }

  @Test
  fun `test navigate to method`() = doTest("psi_element://Foo#foo", """
    class Foo(val property: Int) {
      def <caret>foo(): Unit = {}
    }
  """) {
    scala("Foo.scala", """
      class Foo(val property: Int) {
        def foo(): Unit = {}
      }
    """)
    scala("Bar.scala", """
      class Bar(val property: Int) {
        def bar(): Unit = {}
      }
    """)
  }

  @Test
  fun `test navigate to property`() = doTest("psi_element://Bar#property", """
    class Bar(val <caret>property: Int) {
      def bar(): Unit = {}
    }
  """) {
    scala("Foo.scala", """
      class Foo(val property: Int) {
        def foo(): Unit = {}
      }
    """)
    scala("Bar.scala", """
      class Bar(val property: Int) {
        def bar(): Unit = {}
      }
    """)
  }
}
