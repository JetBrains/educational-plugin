package com.jetbrains.edu.learning.eduAssistant.core

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.getFunctionSignaturesFromGeneratedCode
import com.jetbrains.edu.learning.eduAssistant.core.prompt.CompilationErrorPromptBuilder
import com.jetbrains.edu.learning.eduAssistant.core.prompt.NextStepHintPromptBuilder
import com.jetbrains.edu.learning.eduAssistant.core.prompt.PromptBuilder
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformAdapter
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformException
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_CODE_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_TEXT_HINT
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.eduAssistant.log.Loggers
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.rd.util.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class TaskBasedAssistant : Assistant {

  private var currentTaskFile: TaskFile? = null
  private var promptBuilder: PromptBuilder = NextStepHintPromptBuilder()

  override suspend fun getHint(taskProcessor: TaskProcessor, state: EduState, userCode: String?): AssistantResponse {
    val task = taskProcessor.task
    logEduAssistantInfo(taskProcessor, "Next step hint request")
    Loggers.hintTimingLogger.info("Starting getHint function for task-id: ${task.id}")

    return try {
      Loggers.hintTimingLogger.info("Retrieving the student's code for task-id: ${task.id}")
      var codeStr = ""
      if (taskProcessor.getFailureMessage() == EduCoreBundle.message("check.error.compilation.failed")) {
        promptBuilder = CompilationErrorPromptBuilder()
        codeStr = taskProcessor.getFullTaskFileText(state)
      } else {
        userCode?.let {
          codeStr = it
        } ?: ApplicationManager.getApplication().invokeAndWait {
          codeStr = taskProcessor.getSubmissionTextRepresentation(state) ?: ""
        }
      }
      logEduAssistantInfo(
        taskProcessor,
        """User code:
          |$codeStr
        """.trimMargin()
      )

      val nextStepCodeHintPrompt = generateCodeHintPrompt(taskProcessor, null, codeStr)
      val codeHint = generateCodeHint(taskProcessor, state, nextStepCodeHintPrompt, codeStr)
      return generateFinalHintsAndResponse(taskProcessor, codeHint, codeStr, nextStepCodeHintPrompt, state)
    }
    // TODO: Handle more exceptions with AiPlatformException
    catch (e: AiPlatformException) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      AssistantResponse(assistantError = e.assistantError)
    }
    catch (e: Throwable) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      AssistantResponse(assistantError = AssistantError.UnknownError)
    }
  }

  private fun getEnhancedCodeHint(taskProcessor: TaskProcessor, functionName: String, codeStr: String, codeHint: String, state: EduState): String {
    val task = taskProcessor.task
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    Loggers.hintTimingLogger.info("Retrieving the code hint from solution if it is a short function for task-id: ${task.id}")
    val nextStepCodeFromSolution = taskProcessor.getShortFunctionFromSolutionIfRecommended(codeHint, project, languageId, functionName, currentTaskFile ?: state.taskFile)
    if (nextStepCodeFromSolution != null) {
      logEduAssistantInfo(
        taskProcessor,
        """Code hint from solution:
             |$nextStepCodeFromSolution
          """.trimMargin()
      )
      return nextStepCodeFromSolution
    } else {
      Loggers.hintTimingLogger.info("Retrieving the function psi from student code for task-id: ${task.id}")
      val functionFromCode = taskProcessor.getFunctionPsiWithName(codeStr, functionName, project, languageId)
      Loggers.hintTimingLogger.info("Retrieving the function psi from code hint for task-id: ${task.id}")
      val functionFromCodeHint = taskProcessor.getFunctionPsiWithName(codeHint, functionName, project, languageId)
      Loggers.hintTimingLogger.info("Reducing the code hint for task-id: ${task.id}")
      val reducedCodeHint = taskProcessor.reduceChangesInCodeHint(functionFromCode?.copy(), functionFromCodeHint?.copy(), project, languageId)
      Loggers.hintTimingLogger.info("Applying inspections to the code hint for task-id: ${task.id}")
      //TODO: investigate wrong cases
      val nextStepCodeHint = applyInspections(reducedCodeHint, project, languageId)
      logEduAssistantInfo(
        taskProcessor,
        """Final code hint:
             |$nextStepCodeHint
          """.trimMargin()
      )
      return nextStepCodeHint
    }
  }

  private fun generateCodeHintPrompt(taskProcessor: TaskProcessor, nextStepTextHint: String?, codeStr: String): String {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the next step code hint prompt for task-id: ${task.id}")
    val language = task.course.languageDisplayName.lowercase()
    val nextStepCodeHintPrompt = nextStepTextHint?.let {
      promptBuilder.buildCodeHintPromptFromTextHint(taskProcessor, it, codeStr, language)
    } ?: run {
      promptBuilder.buildCodeHintPrompt(taskProcessor, codeStr, language)
    }
    logEduAssistantInfo(
      taskProcessor,
      """Code hint prompt:
             |$nextStepCodeHintPrompt
          """.trimMargin()
    )
    return nextStepCodeHintPrompt
  }

  private suspend fun generateCodeHint(taskProcessor: TaskProcessor, state: EduState, nextStepCodeHintPrompt: String, codeStr: String): String? {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the code hint for task-id: ${task.id}")
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    val codeHint = taskProcessor.extractRequiredFunctionsFromCodeHint(
      getNextStepCodeHint(nextStepCodeHintPrompt, project, languageId),
    ).also {
      logEduAssistantInfo(
        taskProcessor,
        """Code hint response (before applying inspections):
            |$it
          """.trimMargin()
      )
      Loggers.hintTimingLogger.info("Received the code hint for task-id: ${task.id}")
    }
    if (codeHint.isBlank()) {
      return null
    }
    try {
      Loggers.hintTimingLogger.info("Retrieving the modified function name for task-id: ${task.id}")
      val functionName = taskProcessor.getModifiedFunctionNameInCodeHint(codeStr, codeHint)
      currentTaskFile = task.taskFiles[task.taskFilesWithChangedFunctions?.filter { (_, functions) ->
        functionName in functions
      }?.firstOrNull()?.key ?: state.taskFile.name] ?: state.taskFile
      return if (promptBuilder is NextStepHintPromptBuilder) {
        getEnhancedCodeHint(taskProcessor, functionName, codeStr, codeHint, state)
      } else {
        codeHint
      }
    } catch (e: IllegalStateException) {
      Loggers.eduAssistantLogger.error("Error occurred: ${e.stackTraceToString()}")
      return null
    }
  }

  private suspend fun generateFinalHintsAndResponse(
    taskProcessor: TaskProcessor,
    codeHint: String?,
    codeStr: String,
    codeHintPrompt: String,
    state: EduState,
  ): AssistantResponse {
    val task = taskProcessor.task
    Loggers.hintTimingLogger.info("Retrieving the text hint prompt for task-id: ${task.id}")
    val language = task.course.languageDisplayName.lowercase()
    val nextStepTextHintPrompt = codeHint?.let {
      promptBuilder.buildTextHintPrompt(taskProcessor, it, codeStr, language)
    } ?: run {
      logEduAssistantInfo(taskProcessor, "The code hint was not generated, so the text hint is generated first")
      promptBuilder.buildTextHintPromptIfNoCodeHintIsGenerated(taskProcessor, codeStr, language)
    }
    logEduAssistantInfo(
      taskProcessor,
      """Text hint prompt:
          |$nextStepTextHintPrompt
        """.trimMargin()
    )
    Loggers.hintTimingLogger.info("Retrieving the text hint for task-id: ${task.id}")
    val nextStepTextHint = AiPlatformAdapter.chat(userPrompt = nextStepTextHintPrompt, generationContextProfile = NEXT_STEP_TEXT_HINT)
    logEduAssistantInfo(
      taskProcessor,
      """Text hint response:
          |$nextStepTextHint
        """.trimMargin()
    )
    Loggers.hintTimingLogger.info("Received the text hint for task-id: ${task.id}")

    val nextStepCodeHintPrompt = codeHint?.let { codeHintPrompt } ?: run {
      generateCodeHintPrompt(taskProcessor, nextStepTextHint, codeStr)
    }
    val nextStepCodeHint = codeHint ?: run {
      logEduAssistantInfo(taskProcessor, "The code hint is generated for the second time")
      generateCodeHint(taskProcessor, state, nextStepCodeHintPrompt, codeStr)
    }

    val prompts = mapOf(
      "nextStepTextHintPrompt" to nextStepTextHintPrompt,
      "nextStepCodeHintPrompt" to nextStepCodeHintPrompt
    )
    return AssistantResponse(nextStepTextHint, nextStepCodeHint?.let {
      taskProcessor.applyCodeHint(it, currentTaskFile ?: state.taskFile) }, currentTaskFile, prompts).also {
      Loggers.hintTimingLogger.info("Completed getHint function for task-id: ${task.id}")
    }
  }

  private suspend fun getNextStepCodeHint(nextStepCodeHintPrompt: String, project: Project, language: Language, maxAttempts: Int = 3): String {
    return withContext(Dispatchers.Default) {
      var lastNextStepCodeHint: String? = null
      repeat(maxAttempts) { _ ->
        val nextStepCodeHint = AiPlatformAdapter.chat(
          userPrompt = nextStepCodeHintPrompt,
          temp = 0.1,
          generationContextProfile = NEXT_STEP_CODE_HINT
        )
        if (lastNextStepCodeHint == nextStepCodeHint) {
          error("Failed to generate the code hint.")
        }
        val result = getCodeFromResponse(nextStepCodeHint, language.displayName.lowercase())
        if (!result.isNullOrBlank() && getFunctionSignaturesFromGeneratedCode(result, project, language).isNotEmpty()) {
          return@withContext result
        }
        lastNextStepCodeHint = nextStepCodeHint
      }
      error("Failed to generate the code hint.")
    }
  }

  private fun getCodeFromResponse(response: String, language: String): String? {
    val pattern = """```$language(.*?)```""".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))
    return pattern.find(response)?.groups?.get(1)?.value?.trim()
  }

  companion object {
    fun logEduAssistantInfo(taskProcessor: TaskProcessor, message: String) = Loggers.eduAssistantLogger.info(
      """Lesson id: ${taskProcessor.task.lesson.id}    Task id: ${taskProcessor.task.id}
        |$message
        |
      """.trimMargin()
    )
  }
}
