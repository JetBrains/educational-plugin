package com.jetbrains.edu.kotlin

import com.jetbrains.edu.learning.courseLoading.BundledCoursesProvider


class KtBundledCoursesProvider: BundledCoursesProvider() {
  override fun getBundledCoursesNames(): List<String> = listOf("Kotlin Koans.zip")
}