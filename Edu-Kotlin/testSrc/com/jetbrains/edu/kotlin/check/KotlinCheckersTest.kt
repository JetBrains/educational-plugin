package com.jetbrains.edu.kotlin.check

import com.jetbrains.edu.kotlin.KtProjectGenerator
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.test.CheckActionListener
import org.junit.Assert

class KotlinCheckersTest : CheckersTestBase() {

    fun testKotlinCourse() {
        doTest()
    }

    fun testErrors() {
        CheckActionListener.afterCheck = { task ->
            val taskName = task.lesson.name + "/" + task.name
            Assert.assertFalse("Check Task Action skipped for " + taskName, task.status == CheckStatus.Unchecked)
            Assert.assertFalse("Check Task Action passed for " + taskName, task.status == CheckStatus.Solved)
            println("Check for $taskName fails as expected")
            false
        }
        doTest()
    }

    override fun getGenerator(course: Course) = KtProjectGenerator(course)
}
