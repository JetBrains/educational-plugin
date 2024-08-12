package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat.Companion.TASK_DESCRIPTION_PREFIX
import com.jetbrains.educational.translation.enum.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@Suppress("Junit4RunWithInspection")
@RunWith(Parameterized::class)
class DescriptionFormatTest(private val languageCode: String) : EduTestCase() {
  @Test
  fun `test HTML translated description file`() {
    val fileName = "${TASK_DESCRIPTION_PREFIX}_$languageCode.${DescriptionFormat.HTML.extension}"
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), true)
  }

  @Test
  fun `test MD translated description file`() {
    val fileName = "${TASK_DESCRIPTION_PREFIX}_$languageCode.${DescriptionFormat.MD.extension}"
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), true)
  }

  @Test
  fun `test wrong HTML translated description file`() {
    val fileName = "task1_$languageCode.${DescriptionFormat.HTML.extension}"
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), false)
  }

  @Test
  fun `test wrong MD translated description file`() {
    val fileName = "${TASK_DESCRIPTION_PREFIX}_${languageCode}something.${DescriptionFormat.MD.extension}"
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), false)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "{0}")
    @OptIn(ExperimentalStdlibApi::class)
    fun data(): Collection<Array<Any>> = Language.entries.map { arrayOf(it.code) }
  }
}