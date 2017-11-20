package com.jetbrains.edu.kotlin.check

import com.jetbrains.edu.kotlin.KtProjectGenerator
import com.jetbrains.edu.learning.courseFormat.Course

class KotlinCheckersTest : CheckersTestBase() {

    fun testKotlinCourse() {
        doTest()
    }

    override fun getGenerator(course: Course) = KtProjectGenerator(course)
}
