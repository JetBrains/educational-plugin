package com.jetbrains.edu.ai.tests

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.writeCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.document
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.http.entity.ContentType
import java.util.concurrent.TimeUnit

/**
 * Right-click on the task directory and select 'Generate Tests for This Task' to run this action.
 */
class GenerateTaskTests : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val data = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    e.presentation.isEnabledAndVisible = (data?.findChild("task.md")
                                          ?: data?.findChild("task.html")) != null
    e.presentation.icon = AllIcons.RunConfigurations.TestState.Run_run
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!
    val taskRoot = e.dataContext.getData(CommonDataKeys.VIRTUAL_FILE) ?: error("No task root found")
    val taskFile = taskRoot.findChild("task.md") ?: taskRoot.findChild("task.html") ?: error("No task file found")
    val generationData = computeGenerationData(taskRoot)

    runWithModalProgressBlocking(project, "Generating tests") {
      val prompt = buildPrompt(taskFile, generationData)
      val generatedCode = sendRequest(prompt)

      writeCommandAction(project, "Applying Generated Code") {
        generationData.testFile.document.setText(generatedCode)
      }

      withContext(Dispatchers.EDT) {
        FileEditorManager.getInstance(project).openFile(generationData.testFile, true)
      }
    }
  }

  private suspend fun sendRequest(prompt: String): String {
    return withContext(Dispatchers.IO) {
      val token = System.getenv("EDU_TOOLS_TEST_GENERATION_TOKEN") // Token for Google Gemini
      val request = Request.Builder()
        .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$token")
        .header("Content-Type", "application/json")
        .method("POST", prompt.toRequestBody(ContentType.APPLICATION_JSON.mimeType.toMediaType()))
        .build()

      OkHttpClient.Builder()
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
        .newCall(request)
        .execute().use { response ->
          if (response.code != 200) error("Failed to generate tests: ${response.code}")
          parseResponse(response.body?.string()!!)
        }
    }
  }

  private fun parseResponse(response: String): String {
    val result = StringBuilder()
    val mapper = ObjectMapper()
    val root = mapper.readTree(response) as ObjectNode

    root["candidates"][0]["content"]["parts"].forEach {
      result.append(it["text"].asText())
    }
    return result.toString()
  }


  private fun buildPrompt(taskFile: VirtualFile, generationData: GenerationData): String {
    val descriptionText = taskFile.readText().replace("\"", "\\\"")
    val codeSnippet = generationData.codeFile.readText().replace("\"", "\\\"")

    val task = """
      You are helping a teacher to write unit tests for their code.
      You will be given an exercise description in block starting with '--- DESCRIPTION ---' and ending with '--- END DESCRIPTION ---'.
      You will be given a code snippet starting with '--- CODE ---' and ending with '--- END CODE ---'. This code snippet is given to the user so that they can modify it in a way, that will pass tests.
      Your task is to generate unit tests for the exercise taking description and code snippet into account. 
      ${generationData.promptCustomization}
      Respond with code only, nothing else. Under no circumstances DO NOT include code escape characters like '```kotlin' or '```python' in your response.
      --- DESCRIPTION ---
      $descriptionText
      --- END DESCRIPTION ---
      --- CODE ---
      $codeSnippet
      --- END CODE ---
    """.trimIndent()

    // Google Gemini specific request
    return """
      {
  "contents": [{
    "parts":[{"text": "$task"}]
    }]
   }
    """.trimIndent()
  }

  private fun computeGenerationData(directory: VirtualFile): GenerationData {
    val kotlinFile = directory.findChild("src")?.findChild("Task.kt")
    if (kotlinFile != null) {
      val testFile = directory.findChild("test")?.findOrCreateChildData(this, "Tests.kt")!!
      return GenerationData("Kotlin",
        kotlinFile,
        testFile,
        "Tests must be in kotlin, inside a class called 'Tests' and must use Junit library and '@Test' annotation for evey test. If the code snippet requires the user to implement a function or a class, to you have to import this function or class in tests.")
    }

    val pythonFile = directory.findChild("task.py")
    if (pythonFile != null) {
      val testFile = directory.findChild("tests")?.findOrCreateChildData(this, "test_task.py")!!
      return GenerationData("Python",
        pythonFile,
        testFile,
        "Tests must be in Python and use unittest library for tests. If the code snippet requires the user to implement a function or a class, to you have to import this function or class in tests from the file `..task.py`.")
    }

    error("Can't find kotlin or python file in task")
  }


  data class GenerationData(val language: String, val codeFile: VirtualFile, val testFile: VirtualFile, val promptCustomization: String)
}