package com.jetbrains.edu.rust

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.rust.checker.RsTaskCheckerProvider
import org.rust.cargo.CargoConstants
import org.rust.ide.icons.RsIcons
import javax.swing.Icon

class RsConfigurator : EduConfigurator<RsProjectSettings> {

    private val builder: RsCourseBuilder = RsCourseBuilder()
    private val taskCheckerProvider: RsTaskCheckerProvider = RsTaskCheckerProvider()

    override fun getTaskCheckerProvider(): TaskCheckerProvider = taskCheckerProvider

    override fun getTestFileName(): String = ""

    override fun getCourseBuilder(): EduCourseBuilder<RsProjectSettings> = builder

    override fun getTestDirs(): List<String> = listOf("tests")
    override fun getSourceDir(): String = "src"

    override fun getLogo(): Icon = RsIcons.RUST

    override fun excludeFromArchive(project: Project, file: VirtualFile): Boolean {
        if (super.excludeFromArchive(project, file)) return true
        if (file.name == CargoConstants.LOCK_FILE) return true
        return generateSequence(file, VirtualFile::getParent).any { it.name == CargoConstants.ProjectLayout.target }
    }
}
