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
import com.jetbrains.edu.learning.eduAssistant.grazie.GenerationContextProfile.*
import com.jetbrains.edu.learning.eduAssistant.inspection.applyInspections
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.messages.EduCoreBundle
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    logEduAssistantInfo(
      task,
      """Task analysis prompt:
        |$taskAnalysisPrompt
      """.trimMargin()
    )

    taskAnalysisTimingLogger.info { "Requesting model for task analysis for task-id: ${task.id}" }

    return AiPlatformAdapter.chat(
      userPrompt = taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized"),
      generationContextProfile = SOLUTION_STEPS
    ).deleteNoCodeSteps()
      .also {
      logEduAssistantInfo(
        task,
        """Task analysis response:
            |$it
          """.trimMargin()
      )
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

  fun String.parseSteps() = deleteNoCodeSteps().lines()

  override suspend fun getHint(task: Task, state: EduState, userCode: String?): AssistantResponse {
    logEduAssistantInfo(task, "Next step hint request")
    hintTimingLogger.info { "Starting getHint function for task-id: ${task.id}" }

    return try {
      if (taskProcessor.getFailureMessage() == EduCoreBundle.message("error.execution.failed")) return AssistantResponse(
         assistantError = AssistantError.NoCompiledCode
      )

      hintTimingLogger.info { "Retrieving the student's code for task-id: ${task.id}" }
      var codeStr = ""
      userCode?.let {
        codeStr = it
      } ?: ApplicationManager.getApplication().invokeAndWait {
        codeStr = taskProcessor.getSubmissionTextRepresentation(state) ?: ""
      }
      logEduAssistantInfo(
        task,
        """User code:
          |$codeStr
        """.trimMargin()
      )

      hintTimingLogger.info { "Retrieving the task analysis for task-id: ${task.id}" }
      // The task files were not changed and the user asked about help again
      if (!taskProcessor.hasFilesChanged() && task.aiAssistantState == AiAssistantState.HelpAsked) getTaskAnalysis(task)
      val taskAnalysis = task.generatedSolutionSteps?.also { hintContext?.log() } ?: getTaskAnalysis(task) ?: return AssistantResponse(
        assistantError = AssistantError.NoCompiledCode
      )

      val nextStepCodeHintPrompt = generateCodeHintPrompt(null, task, taskAnalysis, codeStr)
      val codeHint = generateCodeHint(task, state, nextStepCodeHintPrompt, codeStr)
      return generateFinalHintsAndResponse(codeHint, codeStr, task, nextStepCodeHintPrompt, state, taskAnalysis)
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

  private fun getEnhancedCodeHint(functionName: String, codeStr: String, codeHint: String, task: Task, state: EduState): String {
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    hintTimingLogger.info { "Retrieving the code hint from solution if it is a short function for task-id: ${task.id}" }
    val nextStepCodeFromSolution = taskProcessor.getShortFunctionFromSolutionIfRecommended(codeHint, project, languageId, functionName, state.taskFile)
    if (nextStepCodeFromSolution != null) {
      logEduAssistantInfo(
        task,
        """Code hint from solution:
             |$nextStepCodeFromSolution
          """.trimMargin()
      )
      return nextStepCodeFromSolution
    } else {
      hintTimingLogger.info { "Retrieving the function psi from student code for task-id: ${task.id}" }
      val functionFromCode = taskProcessor.getFunctionPsiWithName(codeStr, functionName, project, languageId)
      hintTimingLogger.info { "Retrieving the function psi from code hint for task-id: ${task.id}" }
      val functionFromCodeHint = taskProcessor.getFunctionPsiWithName(codeHint, functionName, project, languageId)
      hintTimingLogger.info { "Reducing the code hint for task-id: ${task.id}" }
      val reducedCodeHint = taskProcessor.reduceChangesInCodeHint(functionFromCode?.copy(), functionFromCodeHint?.copy(), project, languageId)
      hintTimingLogger.info { "Applying inspections to the code hint for task-id: ${task.id}" }
      //TODO: investigate wrong cases
      val nextStepCodeHint = applyInspections(reducedCodeHint, project, languageId)
      logEduAssistantInfo(
        task,
        """Final code hint:
             |$nextStepCodeHint
          """.trimMargin()
      )
      return nextStepCodeHint
    }
  }

  private fun generateCodeHintPrompt(nextStepTextHint: String?, task: Task, taskAnalysis: String, codeStr: String): String {
    hintTimingLogger.info { "Retrieving the next step code hint prompt for task-id: ${task.id}" }
    val language = task.course.languageDisplayName.lowercase()
    val nextStepCodeHintPrompt = nextStepTextHint?.let {
      buildNextStepCodeHintPromptFromTextHint(it, codeStr, language)
    } ?: run {
      buildNextStepCodeHintPrompt(taskAnalysis, codeStr, language)
    }
    logEduAssistantInfo(
      task,
      """Code hint prompt:
             |$nextStepCodeHintPrompt
          """.trimMargin()
    )
    return nextStepCodeHintPrompt
  }

  private suspend fun generateCodeHint(task: Task, state: EduState, nextStepCodeHintPrompt: String, codeStr: String): String? {
    hintTimingLogger.info { "Retrieving the code hint for task-id: ${task.id}" }
    val project = task.project ?: error("Project was not found")
    val languageId = task.course.languageById ?: error("Language was not found")
    val codeHint = taskProcessor.extractRequiredFunctionsFromCodeHint(
      getNextStepCodeHint(nextStepCodeHintPrompt, project, languageId),
      state.taskFile
    ).also {
      logEduAssistantInfo(
        task,
        """Code hint response (before applying inspections):
            |$it
          """.trimMargin()
      )
      hintTimingLogger.info { "Received the code hint for task-id: ${task.id}" }
    }
    if (codeHint.isBlank()) {
      return null
    }
    try {
      hintTimingLogger.info { "Retrieving the modified function name for task-id: ${task.id}" }
      val functionName = taskProcessor.getModifiedFunctionNameInCodeHint(codeStr, codeHint)
      return getEnhancedCodeHint(functionName, codeStr, codeHint, task, state)
    } catch (e: IllegalStateException) {
      logger.error { "Error occurred: ${e.stackTraceToString()}" }
      return null
    }
  }

  private suspend fun generateFinalHintsAndResponse(codeHint: String?, codeStr: String, task: Task, codeHintPrompt: String, state: EduState, taskAnalysis: String): AssistantResponse {
    hintTimingLogger.info { "Retrieving the text hint prompt for task-id: ${task.id}" }
    val nextStepTextHintPrompt = codeHint?.let {
      buildNextStepTextHintPrompt(it, codeStr)
    } ?: run {
      logEduAssistantInfo(task, "The code hint was not generated, so the text hint is generated first")
      buildNextStepTextHintPromptIfNoCodeHintIsGenerated(taskAnalysis, codeStr)
    }
    logEduAssistantInfo(
      task,
      """Text hint prompt:
          |$nextStepTextHintPrompt
        """.trimMargin()
    )
    hintTimingLogger.info { "Retrieving the text hint for task-id: ${task.id}" }
    val nextStepTextHint = AiPlatformAdapter.chat(userPrompt = nextStepTextHintPrompt, generationContextProfile = NEXT_STEP_TEXT_HINT)
    logEduAssistantInfo(
      task,
      """Text hint response:
          |$nextStepTextHint
        """.trimMargin()
    )
    hintTimingLogger.info { "Received the text hint for task-id: ${task.id}" }

    taskAnalysisPrompt ?: run {
      hintContext ?: run {
        hintContext = buildHintContext(task)
        taskAnalysisPrompt = buildTaskAnalysisPrompt(hintContext ?: error("Empty hintContext"))
      }
    }

    val nextStepCodeHintPrompt = codeHint?.let { codeHintPrompt } ?: run {
      generateCodeHintPrompt(nextStepTextHint, task, taskAnalysis, codeStr)
    }
    val nextStepCodeHint = codeHint ?: run {
      logEduAssistantInfo(task, "The code hint is generated for the second time")
      generateCodeHint(task, state, nextStepCodeHintPrompt, codeStr)
    }

    val prompts = mapOf(
      "taskAnalysisPrompt" to (taskAnalysisPrompt ?: error("taskAnalysisPrompt was not initialized!")),
      "nextStepTextHintPrompt" to nextStepTextHintPrompt,
      "nextStepCodeHintPrompt" to nextStepCodeHintPrompt
    )
    return AssistantResponse(nextStepTextHint, nextStepCodeHint?.let { taskProcessor.applyCodeHint(it, state.taskFile) }, prompts).also {
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
    Based on the given code and the improved version of the code, provide a concise textual hint that directly guides to improve the given code.
    Here is the current code and the improved version of the code, all delimited with <>:
    
    The code:
    <$codeStr>
    
    The improved version of the code:
     <$nextStepCodeHint>
    
    Respond with a brief textual instruction in imperative form of what modifications need to be made to the code to achieve the improvements exhibited in the improved code. 
    Do not write any code, except names of functions or string literals. 
    Avoid explaining why the modifications are needed.
  """.trimIndent()

  private fun buildNextStepTextHintPromptIfNoCodeHintIsGenerated(taskAnalysis: String, codeStr: String) = """
    Based on a sequence of steps to solve a coding problem, determine the next step that must be taken to complete the task.
    Here is the list of steps that solves the problem and the code, all delimited with <>:
    
    How to solve the task:
    <$taskAnalysis>
    
    The code:
    <$codeStr>
    
    ${buildTaskErrorInformation()}
    
    Respond with a brief textual instruction in imperative form of what to do next in the problem-solving sequence.
    The response should not provide the complete solution, but instead focus on the next concise step that needs to be taken to make progress.
    Don't use the number of step in the response. Do not write any code, except names of functions that can be used in the solution.
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

  private fun formatCodeResponsePrompt(description: String, codeStr: String, language: String) = """
    $description
    
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

  private fun buildNextStepCodeHintPrompt(
    taskAnalysis: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Generate a modified version of the provided student's code that incorporates the next step towards the solution based on the provided solution steps. 
    The modified code should not be a complete solution but should represent the next logical step that the student needs to take to solve the task. 
    Try to maintain the original structure of the student's code and focus especially on addressing common errors, while guiding the student towards the correct implementation.
    Here is the list of steps that solves the task and the code, all delimited with <>:
    
    The list of steps that solves the task:
    <$taskAnalysis>
  """.trimIndent(), codeStr, language)

  private fun buildNextStepCodeHintPromptFromTextHint(
    nextStepTextHint: String, codeStr: String, language: String
  ) = formatCodeResponsePrompt("""
    Implement the next step to solve the coding task in the present code.
    Here is the description of the next step and the code, all delimited with <>:
    
    Next step: <$nextStepTextHint>
  """.trimIndent(), codeStr, language)

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
        logEduAssistantInfo(
          taskProcessor.task,
          """Hint context - $name:
            |$it
          """.trimMargin()
        )
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

  private fun logEduAssistantInfo(task: Task, message: String) = logger.info {
    """Lesson id: ${task.lesson.id}    Task id: ${task.id}
      |$message
      |
    """.trimMargin()
  }
}
