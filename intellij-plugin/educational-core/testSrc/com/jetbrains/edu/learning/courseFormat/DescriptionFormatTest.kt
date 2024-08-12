package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtilsKt
import org.junit.Test

class DescriptionFormatTest: EduTestCase() {
  @Test
  fun `test HTML description file`() {
    val fileName = DescriptionFormat.HTML.fileName
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), true)
  }

  @Test
  fun `test MD description file`() {
    val fileName = DescriptionFormat.MD.fileName
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), true)
  }

  @Test
  fun `test wrong HTML description file`() {
    val fileName = "${DescriptionFormat.TASK_DESCRIPTION_PREFIX}123.${DescriptionFormat.HTML.extension}"
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), false)
  }

  @Test
  fun `test wrong MD description file`() {
    val fileName = "${DescriptionFormat.TASK_DESCRIPTION_PREFIX}abc.${DescriptionFormat.MD.extension}"
    println(fileName)
    assertEquals(EduUtilsKt.isTaskDescriptionFile(fileName), false)
  }
}