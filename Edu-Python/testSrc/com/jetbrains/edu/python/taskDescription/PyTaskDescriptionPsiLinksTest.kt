package com.jetbrains.edu.python.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionPsiLinksTestBase
import com.jetbrains.python.PythonFileType

class PyTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = PythonFileType.INSTANCE

  fun `test navigate to class`() = doTest("bar.Bar", """
      class <caret>Bar:
          def bar(self):
              print "Bar"
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                print "Foo"
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                print "Bar"
    """)
  }

  fun `test navigate to method`() = doTest("foo.Foo#foo", """
      class Foo:
          def <caret>foo(self):
              print "Foo"
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                print "Foo"
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                print "Bar"
    """)
  }

  fun `test navigate to function`() = doTest("baz", """
      class Bar:
          def bar(self):
              print "Bar"

      def <caret>baz():
          print "baz"
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                print "Foo"
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                print "Bar"

        def baz():
            print "baz"
    """)
  }
}
