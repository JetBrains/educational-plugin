package com.jetbrains.edu.learning.eduAssistant.core

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduState
import com.jetbrains.edu.learning.courseFormat.eduAssistant.AiAssistantState
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.buildAuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.getFunctionSignaturesFromGeneratedCode
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformAdapter
import com.jetbrains.edu.learning.eduAssistant.grazie.AiPlatformException
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_TEXT_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.NEXT_STEP_CODE_HINT
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.SOLUTION_STEPS

class TaskBasedAssistant(private val taskProcessor: TaskProcessor) : Assistant {

  val logger = KotlinLogging.logger("EduAssistantLogger")
  private val taskAnalysisTimingLogger = KotlinLogging.logger("TaskAnalysisTimingLogger")
  private val hintTimingLogger = KotlinLogging.logger("HintTimingLogger")
  var taskAnalysisPrompt: String? = null
  private var hintContext: HintContext? = null

  suspend fun getTaskAnalysis(task: Task): String? {
    taskAnalysisTimingLogger.info { "Starting getTaskAnalysis function for task-id: ${task.id}" }

    if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return null

    task.authorSolutionContext ?: let {
      task.authorSolutionContext = task.buildAuthorSolutionContext()
    }

    hintContext = buildHintContext(task)
    taskAnalysisPrompt = buildTaskAnalysisPrompt(hintContext ?: error("Empty hintContext"))

    logger.info {
      """Task analysis prompt:
        |$taskAnalysisPrompt
      """.trimMargin()
    }

    taskAnalysisTimingLogger.info { "Requesting model for task analysis" }

    return AiPlatformAdapter.chat(
      userPrompt = taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized"),
      generationContextProfile = SOLUTION_STEPS
    ).deleteNoCodeSteps()
      .also {
      logger.info {
        """Task analysis response:
            |$it
          """.trimMargin()
      }
      taskAnalysisTimingLogger.info { "Completed task analysis for task-id: ${task.id}" }
      task.generatedSolutionSteps = it
    }
  }

  private fun String.deleteNoCodeSteps(): String {
    val responseWithoutNonCodeSteps = this.replace(
      Regex("(?i)\\d+\\..*?no-code[\\s\\S]*?(?=\\n\\d+\\.|$)"),
      ""
    ).lines().filterNot { it.isBlank() }.joinToString(System.lineSeparator())
    var index = 1
    return Regex("""(?m)^\d+\.""").replace(responseWithoutNonCodeSteps) { "${index++}." }
  }

  override suspend fun getHint(task: Task, state: EduState, userCode: String?): AssistantResponse {
    logger.info {
      """Next step hint request
        |Lesson id: ${task.lesson.id}
        |Task id: ${task.id}
      """.trimMargin()
    }
    hintTimingLogger.info { "Starting getHint function for task-id: ${task.id}" }

    return try {
      if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return AssistantResponse(
         assistantError = AssistantError.NoCompiledCode
      )

      hintTimingLogger.info { "Retrieving the student's code" }
      var codeStr = ""
      userCode?.let {
        codeStr = it
      } ?: ApplicationManager.getApplication().invokeAndWait {
        codeStr = taskProcessor.getSubmissionTextRepresentation(state) ?: ""
      }
      logger.info {
        """User code:
          |$codeStr
        """.trimMargin()
      }

      hintTimingLogger.info { "Retrieving the task analysis" }
      // The task files were not changed and the user asked about help again
      if (!taskProcessor.hasFilesChanged() && task.aiAssistantState == AiAssistantState.HelpAsked) getTaskAnalysis(task)
      val taskAnalysis = task.generatedSolutionSteps?.also { hintContext?.log() } ?: getTaskAnalysis(task) ?: return AssistantResponse(
        assistantError = AssistantError.NoCompiledCode
      )

      hintTimingLogger.info { "Retrieving the next step code hint prompt" }
      val language = task.course.languageDisplayName.lowercase()
      val nextStepCodeHintPrompt = buildNextStepCodeHintPrompt(taskAnalysis, codeStr, language)
      logger.info {
        """Code hint prompt:
             |$nextStepCodeHintPrompt
          """.trimMargin()
      }

      hintTimingLogger.info { "Retrieving the code hint" }
      val project = task.project ?: error("Project was not found")
      val languageId = task.course.languageById ?: error("Language was not found")
      val codeHint = taskProcessor.extractRequiredFunctionsFromCodeHint(
        getNextStepCodeHint(nextStepCodeHintPrompt, project, languageId),
        state.taskFile
      ).also {
        logger.info {
          """Code hint response (before applying inspections):
            |$it
          """.trimMargin()
        }
        hintTimingLogger.info { "Received the code hint" }
      }
      hintTimingLogger.info { "Retrieving the modified function name" }
      val functionName = try {
        taskProcessor.getModifiedFunctionNameInCodeHint(codeStr, codeHint)
      } catch (e: IllegalStateException) {
        logger.error { "Error occurred: ${e.stackTraceToString()}" }
        return AssistantResponse(assistantError = AssistantError.EmptyDiff)
      }
      hintTimingLogger.info { "Retrieving the code hint from solution if it is a short function" }
      val nextStepCodeFromSolution = taskProcessor.getShortFunctionFromSolutionIfRecommended(codeHint, project, languageId, functionName, state.taskFile)
      if (nextStepCodeFromSolution != null) {
        logger.info {
          """Code hint from solution:
             |$nextStepCodeFromSolution
          """.trimMargin()
        }

        generateTextHintAndResponse(nextStepCodeFromSolution, codeStr, task, nextStepCodeHintPrompt, state)
      } else {
        hintTimingLogger.info { "Retrieving the function psi from student code" }
        val functionFromCode = taskProcessor.getFunctionPsiWithName(codeStr, functionName, project, languageId)
        hintTimingLogger.info { "Retrieving the function psi from code hint" }
        val functionFromCodeHint = taskProcessor.getFunctionPsiWithName(codeHint, functionName, project, languageId)
        hintTimingLogger.info { "Reducing the code hint" }
        val reducedCodeHint = taskProcessor.reduceChangesInCodeHint(functionFromCode?.copy(), functionFromCodeHint?.copy(), project, languageId)
        hintTimingLogger.info { "Applying inspections to the code hint" }
        //TODO: investigate wrong cases
        val nextStepCodeHint = applyInspections(reducedCodeHint, project, languageId)
        logger.info {
          """Final code hint:
             |$nextStepCodeHint
          """.trimMargin()
        }

        generateTextHintAndResponse(nextStepCodeHint, functionFromCode?.text ?: "", task, nextStepCodeHintPrompt, state)
      }
    }
    // TODO: Handle more exceptions with AiPlatformException
    catch (e: AiPlatformException) {
      logger.error { "Error occurred: ${e.stackTraceToString()}" }
      AssistantResponse(assistantError = e.assistantError)
    }
    catch (e: Throwable) {
      logger.error { "Error occurred: ${e.stackTraceToString()}" }
      AssistantResponse(assistantError = AssistantError.UnknownError)
    }
  }

  private suspend fun generateTextHintAndResponse(nextStepCodeHint: String, codeStr: String, task: Task, nextStepCodeHintPrompt: String, state: EduState): AssistantResponse {
    hintTimingLogger.info { "Retrieving the text hint prompt" }
    val nextStepTextHintPrompt = buildNextStepTextHintPrompt(nextStepCodeHint, codeStr)
    logger.info {
      """Text hint prompt:
          |$nextStepTextHintPrompt
        """.trimMargin()
    }
    hintTimingLogger.info { "Retrieving the text hint" }
    val nextStepTextHint = AiPlatformAdapter.chat(userPrompt = nextStepTextHintPrompt, generationContextProfile = NEXT_STEP_TEXT_HINT)
    logger.info {
      """Text hint response:
          |$nextStepTextHint
        """.trimMargin()
    }
    hintTimingLogger.info { "Received the text hint" }

    taskAnalysisPrompt ?: run {
      hintContext ?: run {
        hintContext = buildHintContext(task)
        taskAnalysisPrompt = buildTaskAnalysisPrompt(hintContext ?: error("Empty hintContext"))
      }
    }

    val prompts = mapOf(
      "taskAnalysisPrompt" to (taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized!")),
      "nextStepTextHintPrompt" to nextStepTextHintPrompt,
      "nextStepCodeHintPrompt" to nextStepCodeHintPrompt
    )
    return AssistantResponse(nextStepTextHint, taskProcessor.applyCodeHint(nextStepCodeHint, state.taskFile), prompts).also {
      hintTimingLogger.info { "Completed getHint function for task-id: ${task.id}" }
    }
  }

  private fun buildTaskAnalysisPrompt(
    taskStr: String,
    functionsSetStrFromUserSolution: String?,
    functionsSetStrFromAuthorSolution: String?,
    hintsStr: String,
    theoryStr: String,
    availableForSolutionStrings: String?,
    language: String
  ) = """
    Generate an ordered list of steps to solve the described coding task with each step marked as either "code" or "no-code" in the title. For example, "no-code" steps might involve actions like "End ... function", "Test ... function", "Read the task description and understand the requirements". 
    Each step is a detailed explanation of one distinct concrete action or operation that is understandable for beginners in $language. Use built-in $language functions in your solution. Do not write any example code blocks, except names of functions that can be used in the solution.
    Be very detailed and precise, include at least 6 steps in the list, the more and clearer the better. 
    Here is information about the task, all blocks delimited with <>:
    
    Task: <$taskStr>
    
    Set of functions that might be implemented: <$functionsSetStrFromAuthorSolution>
    
    Existing functions within the user's implementation can be used for the solution, without needing to describe their implementations: <$functionsSetStrFromUserSolution>
    
    Hints: <$hintsStr>
    
    Theory: <$theoryStr>
    
    The solution can use only these strings: <${availableForSolutionStrings ?: "None"}>
  """.trimIndent()

  private fun buildNextStepTextHintPrompt(nextStepCodeHint: String, codeStr: String) = """
    Based on the given code and the improved version of the code, provide a textual hint that guides to improve the given code.
    Here is the current code and the improved version of the code, all delimited with <>:
    
    The code:
    <$codeStr>
    
    The improved version of the code:
     <$nextStepCodeHint>
    
    Respond with a brief textual explanation in imperative form of what modifications need to be made to the code to achieve the improvements exhibited in the improved code. Do not write any code, except names of functions or string literals.
  """.trimIndent()

  private fun buildTaskErrorInformation() = if (taskProcessor.taskHasErrors()) {
    """
      The present code does not pass test <${taskProcessor.getFailedTestName()}>, the error message is <${taskProcessor.getFailureMessage()}>.
      ${
      if (taskProcessor.getExpectedValue() != null && taskProcessor.getActualValue() != null) {
        """
          The expected test output was:
          ${taskProcessor.getExpectedValue()}
          But the actual test output was:
          ${taskProcessor.getActualValue()}
        """.trimIndent()
      }
      else ""
    }
      If the task appears to be completed, provide a step-by-step guide that explains what changes need to be made to the code to fix the failing test.
    """.trimIndent()
  }
  else {
    ""
  }

  private fun buildNextStepCodeHintPrompt(
    taskAnalysis: String, codeStr: String, language: String
  ) = """
    Generate a modified version of the provided student's code that incorporates the next step towards the solution based on the provided solution steps. 
    The modified code should not be a complete solution but should represent the next logical step that the student needs to take to solve the task. 
    Try to maintain the original structure of the student's code and focus especially on addressing common errors, while guiding the student towards the correct implementation.
    Here is the list of steps that solves the task and the code, all delimited with <>:
    
    The list of steps that solves the task:
    <$taskAnalysis>
    
    The student's code:
    <$codeStr>
    
    ${buildTaskErrorInformation()}
    
    Write the response in the following format:
    Response: <non-code text>
    Code: ```$language
    <code>
    ```
    
    The code response should include the entire function or code block with the new changes incorporated into it. 
  """.trimIndent()

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

  inner class HintContext(
    val taskStr: String,
    val functionsSetStrFromUserSolution: String?,
    val functionsSetStrFromAuthorSolution: String?,
    val hintsStr: String,
    val theoryStr: String,
    val availableForSolutionStrings: String?,
    val language: String
  ) {
    init {
      log()
    }

    fun log() {
      log("TASK TEXT", taskStr)
      log("FUNCTIONS SET FROM USER SOLUTION", functionsSetStrFromUserSolution)
      log("FUNCTIONS SET FROM AUTHOR SOLUTION", functionsSetStrFromAuthorSolution)
      log("TASK HINTS", hintsStr)
      log("THEORY SUMMARY", theoryStr)
      log("STRINGS SET", availableForSolutionStrings)
    }

    private fun log(name: String, value: String?) {
      value?.let {
        logger.info {
          """Hint context - $name:
            |$it
          """.trimMargin()
        }
      }
    }
  }

  private fun buildTaskAnalysisPrompt(hintContext: HintContext): String = buildTaskAnalysisPrompt(
    taskStr = hintContext.taskStr,
    functionsSetStrFromUserSolution = hintContext.functionsSetStrFromUserSolution,
    functionsSetStrFromAuthorSolution = hintContext.functionsSetStrFromAuthorSolution,
    hintsStr = hintContext.hintsStr,
    theoryStr = hintContext.theoryStr,
    availableForSolutionStrings = hintContext.availableForSolutionStrings,
    language = hintContext.language
  )

  private fun buildHintContext(task: Task): HintContext {
    val functionsSetStrFromUserSolution = taskProcessor.getFunctionsFromTask()
    val functionsSetStrFromAuthorSolution = task.authorSolutionContext?.functionSignatures?.filterNot {
      functionsSetStrFromUserSolution?.contains(
        it
      ) == true
    }
    val userStrings = taskProcessor.getStringsFromTask()
    val availableForSolutionStrings = task.authorSolutionContext?.functionsToStringMap?.values?.flatten()?.filterNot {
      userStrings.contains(
        it
      )
    }

    return HintContext(
      taskStr = taskProcessor.getTaskTextRepresentation(),
      functionsSetStrFromUserSolution = functionsSetStrFromUserSolution?.joinToString(separator = System.lineSeparator()),
      functionsSetStrFromAuthorSolution = functionsSetStrFromAuthorSolution?.joinToString(separator = System.lineSeparator()),
      hintsStr = taskProcessor.getHintsTextRepresentation().joinToString(separator = System.lineSeparator()),
      theoryStr = taskProcessor.getTheoryTextRepresentation(),
      availableForSolutionStrings = availableForSolutionStrings?.joinToString(separator = System.lineSeparator()),
      language = task.course.languageDisplayName.lowercase()
    )
  }
}
