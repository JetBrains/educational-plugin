package com.jetbrains.edu.python.learning

import com.jetbrains.edu.learning.courseLoading.BundledCoursesProvider


class PyBundledCoursesProvider : BundledCoursesProvider() {
  override fun getBundledCoursesNames(): List<String> = listOf("Introduction to Python.zip")
}