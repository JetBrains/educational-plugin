package com.jetbrains.edu.scala.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionPsiLinksTestBase
import org.jetbrains.plugins.scala.ScalaFileType

class ScalaTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = ScalaFileType.INSTANCE

  fun `test navigate to class`() = doTest("Bar", """
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

  fun `test navigate to method`() = doTest("Foo#foo", """
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

  fun `test navigate to property`() = doTest("Bar#property", """
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
