package com.jetbrains.edu.kotlin

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.containers.ContainerUtil
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.intellij.EduConfiguratorBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings

open class KtConfigurator : EduConfiguratorBase() {

    override fun excludeFromArchive(path: String): Boolean {
        val excluded = super.excludeFromArchive(path)
        return excluded || path.contains("build") || PathUtil.getFileName(path) in NAMES_TO_EXCLUDE
    }

    private val myCourseBuilder = KtCourseBuilder()

    override fun getCourseBuilder(): EduCourseBuilder<JdkProjectSettings> = myCourseBuilder

    override fun getTestFileName(): String = TESTS_KT

    override fun isTestFile(file: VirtualFile): Boolean {
        val name = file.name
        return TESTS_KT == name || LEGACY_TESTS_KT == name || name.contains(FileUtil.getNameWithoutExtension(TESTS_KT)) && name.contains(EduNames.SUBTASK_MARKER)
    }


    override fun getBundledCoursePaths(): List<String> {
        val bundledCourseRoot = EduUtils.getBundledCourseRoot(KtKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME, KtKotlinKoansModuleBuilder::class.java)
        return listOf(FileUtil.join(bundledCourseRoot.absolutePath, KtKotlinKoansModuleBuilder.DEFAULT_COURSE_NAME))
    }

    companion object {

        @JvmField val LEGACY_TESTS_KT = "tests.kt"
        @JvmField val TESTS_KT = "Tests.kt"
        @JvmField val SUBTASK_TESTS_KT = "Subtask_Tests.kt"
        @JvmField val TASK_KT = "Task.kt"

        private val NAMES_TO_EXCLUDE = ContainerUtil.newHashSet(
                "gradlew", "gradlew.bat", "local.properties", "gradle.properties",
                "build.gradle", "settings.gradle", "gradle-wrapper.jar", "gradle-wrapper.properties")
    }
}
