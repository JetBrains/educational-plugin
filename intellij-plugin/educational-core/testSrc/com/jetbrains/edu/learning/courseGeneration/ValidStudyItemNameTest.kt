package com.jetbrains.edu.learning.courseGeneration

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.convertToValidName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class ValidStudyItemNameTest(
  private val input: String,
  private val expected: String,
  @Suppress("unused") private val testName: String
) : EduTestCase() {
  @Test
  fun `test converting`() {
    val actual = input.convertToValidName()
    assertEquals(expected, actual)
  }

  @Test
  fun `integration test`() {
    courseWithFiles {
      lesson("lesson/name") {
        eduTask(input)
      }
    }

    checkFileTree {
      dir("lesson name") {
        dir(expected) {
          file("task.md")
        }
      }
    }
  }

  companion object {
    private const val DEFAULT_RESULT: String = "Study item"

    @Suppress("NonAsciiCharacters")
    @Parameterized.Parameters(name = "{2}")
    @JvmStatic
    fun data(): Collection<Array<Any>> = listOf(
      // EDU-2620
      arrayOf("""Few words about JavaScript.""", "Few words about JavaScript", "EDU-2620 Trailing dot"),
      // EDU-2895
      arrayOf("""#define Задача B ...""", "#define Задача B", "EDU-2895 Multiple trailing dots and space"),
      // EDU-4761
      arrayOf("""A Journey of a Thousand Miles. . .""", "A Journey of a Thousand Miles", "EDU-4761 Trailing space-separated ellipsis"),
      // IDEA-253884
      arrayOf("""Study item with trailing exclamation! """, "Study item with trailing exclamation",
              "IDEA-253884 Trailing exclamation point with space"),

      arrayOf("""Study item """, DEFAULT_RESULT, "Trailing space"),
      arrayOf(""" Study item""", " Study item", "Starting space"),
      arrayOf("""Study: item :""", "Study  item", "Colons"),
      arrayOf("""Study;item;""", DEFAULT_RESULT, "Semicolons"),
      arrayOf("""Study/item / /""", DEFAULT_RESULT, "Slashes"),
      arrayOf("""Study\item\ \ """, DEFAULT_RESULT, "Backslashes"),
      arrayOf("""Study<item>""", DEFAULT_RESULT, "Less-than and greater-than signs"),
      arrayOf("""Study?item ?""", DEFAULT_RESULT, "Question marks"),
      arrayOf("""Study&item& """, DEFAULT_RESULT, "Ampersands"),
      arrayOf("""Study "*| item """, "Study     item", "Other symbols")
    )
  }
}
