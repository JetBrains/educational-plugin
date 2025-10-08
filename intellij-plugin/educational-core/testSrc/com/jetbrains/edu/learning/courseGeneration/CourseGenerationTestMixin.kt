package com.jetbrains.edu.learning.courseGeneration

import com.intellij.diagnostic.dumpCoroutines
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.observation.Observation
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.disambiguateContents
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import kotlin.time.Duration.Companion.minutes

interface CourseGenerationTestMixin<Settings : EduProjectSettings> {

  val defaultSettings: Settings

  val rootDir: VirtualFile

  fun createCourseStructure(
    course: Course,
    metadata: Map<String, String> = emptyMap(),
    waitForProjectConfiguration: Boolean = true
  ): Project {
    val configurator = course.configurator ?: error("Failed to find `EduConfigurator` for `${course.name}` course")
    val generator = configurator.courseBuilder.getCourseProjectGenerator(course)
                    ?: error("The provided builder returned null as the course project generator")
    val project = invokeAndWaitIfNeeded {
      generator.doCreateCourseProject(rootDir.path, defaultSettings, metadata) ?: error("Cannot create project")
    }

    if (waitForProjectConfiguration) {
      waitForCourseConfiguration(project)
      IndexingTestUtil.waitUntilIndexesAreReady(project)
    }
    // Important condition if you are checking workflow when a user declined user agreement
    if (project.isEduProject()) {
      TaskToolWindowView.getInstance(project).currentTask = project.getCurrentTask()
    }

    return project
  }

  fun assertListOfAdditionalFiles(course: Course, vararg files: Pair<String, Any?>) {
    val filesMap = mapOf(*files)
    assertEquals("Unexpected list of additional files", filesMap.keys, course.additionalFiles.map { it.name }.toSet())

    for (actualFile in course.additionalFiles) {
      val path = actualFile.name

      if (!filesMap.containsKey(path)) fail("Unexpected additional file $path")

      val expectedContents = when (val value = filesMap[path]) {
        is String -> InMemoryTextualContents(value)
        is ByteArray -> InMemoryBinaryContents(value)
        is FileContents -> value
        null -> null
        else -> error("value should be either a String or a ByteArray or a FileContents, got ${value::class.java} instead")
      }
      val actualContents = actualFile.contents.disambiguateContents(path)
      if (expectedContents != null) {
        assertContentsEqual(path, expectedContents, actualContents)
      }
    }
  }

  fun waitForCourseConfiguration(project: Project) {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch { Observation.awaitConfiguration(project) }
    if (ApplicationManager.getApplication().isDispatchThread) {
      PlatformTestUtil.waitForFuture(job.asCompletableFuture())
    }
    else {
      runBlockingMaybeCancellable {
        try {
          // The same timeout as `PlatformTestUtil.MAX_WAIT_TIME`
          withTimeout(2.minutes) {
            job.join()
          }
        }
        catch (e: TimeoutCancellationException) {
          throw RuntimeException("Cannot wait for course configuration to complete\n${dumpCoroutines()}", e)
        }
      }
    }
  }
}
