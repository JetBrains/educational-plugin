package com.jetbrains.edu.rust

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import org.rust.ide.icons.RsIcons
import javax.swing.Icon

class RsConfigurator : EduConfigurator<RsEduSettings> {

    private val builder: RsCourseBuilder = RsCourseBuilder()
    private val taskCheckerProvider: RsTaskCheckerProvider = RsTaskCheckerProvider()

    override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

    override fun getTestFileName(): String = ""

    override fun getCourseBuilder(): EduCourseBuilder<RsEduSettings> = builder

    override fun getTestDirs(): List<String> = listOf("tests")
    override fun getSourceDir(): String = "src"

    override fun getLogo(): Icon = RsIcons.RUST
}
