package com.jetbrains.edu.csharp

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ApplicationRule
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestMixin
import com.jetbrains.rider.test.BaseIntegrationTest
import org.junit.Rule
import org.junit.rules.TestRule
import java.io.File
import java.nio.file.Files

/**
 * Rider-friendly base for C# tests that need course generation helpers but must run under Rider's
 * BaseIntegrationTest infrastructure.
 */
abstract class CSharpCourseGenerationTestBase : BaseIntegrationTest(), CourseGenerationTestMixin<CSharpProjectSettings> {

  // Ensures IntelliJ Application is initialized for JUnit4-based tests
  @Rule
  @JvmField
  val appRule: TestRule = ApplicationRule()

  override val defaultSettings: CSharpProjectSettings = CSharpProjectSettings()

  override val rootDir: VirtualFile by lazy {
    val tmp = Files.createTempDirectory("csharp-course-").toFile()
    val path = tmp.absolutePath.replace(File.separatorChar, '/')

    LocalFileSystem.getInstance().refreshAndFindFileByPath(path) ?: error("Can't create/find temp root dir: $path")
  }
}