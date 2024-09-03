package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.InMemoryUndeterminedContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.storage.debugString
import com.jetbrains.edu.learning.storage.wrapWithDiagnostics
import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class FileContentsDebugStringTests(
  private val expectedDebugString: String,
  private val fileContents: FileContents
) {
  @Test
  fun `debug string for FileContents`() {
    val debugString = fileContents.debugString()
      .replace("hash=[0-9a-z]{8}".toRegex(), "hash=00000000")
      .replace("""[\w.]+(\$[\w.]+)+""".toRegex(), """NAME_WITH_\$""")
    assertEquals(expectedDebugString, debugString)
  }

  companion object {
    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {

      val textualContents = InMemoryTextualContents("abc")
      val binaryContents = InMemoryBinaryContents(byteArrayOf(1, 2, 3, 4))
      val undeterminedContents = InMemoryUndeterminedContents("abcdef")

      return listOf(
        arrayOf("InMemoryTextualContents(size=3 hash=00000000)", textualContents),
        arrayOf("InMemoryBinaryContents(size=4 hash=00000000)", binaryContents),
        arrayOf("InMemoryUndeterminedContents(size=6 hash=00000000)", undeterminedContents),
        arrayOf("[InMemoryTextualContents(size=3 hash=00000000)]", wrapWithDiagnostics(textualContents, "")),
        arrayOf("[InMemoryBinaryContents(size=4 hash=00000000)]", wrapWithDiagnostics(binaryContents, "")),
        arrayOf(
          "[InMemoryUndeterminedContents(size=6 hash=00000000)]",
          wrapWithDiagnostics(undeterminedContents, "")
        ),
        arrayOf(
          "[[InMemoryTextualContents(size=3 hash=00000000)]]",
          wrapWithDiagnostics(wrapWithDiagnostics(textualContents, ""), "")
        ),
        arrayOf(
          "NAME_WITH_$(size=3 hash=00000000)",
          object : TextualContents {
            override val text = "xyz"
          }
        )
      )
    }
  }
}