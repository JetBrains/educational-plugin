package com.jetbrains.edu.python.taskDescription

import com.intellij.openapi.fileTypes.FileType
import com.jetbrains.edu.learning.taskToolWindow.links.TaskDescriptionPsiLinksTestBase
import com.jetbrains.python.PythonFileType
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyTaskDescriptionPsiLinksTest : TaskDescriptionPsiLinksTestBase() {

  override val fileType: FileType = PythonFileType.INSTANCE

  @Test
  fun `test navigate to class`() = doTest("psi_element://bar.Bar", """
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

  @Test
  fun `test navigate to method`() = doTest("psi_element://foo.Foo.foo", """
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

  @Test
  fun `test navigate to function`() = doTest("psi_element://bar.baz", """
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
