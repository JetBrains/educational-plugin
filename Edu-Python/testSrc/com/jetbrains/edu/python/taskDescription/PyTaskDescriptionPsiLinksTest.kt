package com.jetbrains.edu.python.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskDescription.TaskDescriptionPsiLinksTestBase
import com.jetbrains.python.PythonFileType

class PyTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = PythonFileType.INSTANCE

  fun `test navigate to class`() = doTest("bar.Bar", """
      class <caret>Bar:
          def bar(self):
              pass
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                pass
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                pass
    """)
  }

  fun `test navigate to method`() = doTest("foo.Foo.foo", """
      class Foo:
          def <caret>foo(self):
              pass
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                pass
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                pass
    """)
  }

  fun `test navigate to function`() = doTest("bar.baz", """
      class Bar:
          def bar(self):
              pass

      def <caret>baz():
          pass
  """) {
    python("foo.py", """
        class Foo:
            def foo(self):
                pass
    """)
    python("bar.py", """
        class Bar:
            def bar(self):
                pass

        def baz():
            pass
    """)
  }
}
