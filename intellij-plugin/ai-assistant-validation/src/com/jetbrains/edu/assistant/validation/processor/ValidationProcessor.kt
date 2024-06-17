package com.jetbrains.edu.assistant.validation.processor

import com.jetbrains.edu.assistant.validation.util.*
import com.jetbrains.educational.ml.core.grazie.GrazieConnectionManager

private val grazie: GrazieConnectionManager by lazy {
  GrazieConnectionManager.create("user", "learning-assistant-prompt")
}

private const val AUTO_VALIDATION_LLM_PROFILE_ID = "gpt-4o"

suspend fun processValidationHints(
  taskDescription: String,
  textHint: String,
  codeHint: String,
  userCode: String,
) = grazie.chat(
    llmProfileId = AUTO_VALIDATION_LLM_PROFILE_ID,
    systemPrompt = buildHintsValidationSystemPrompt(),
    userPrompt = buildHintsValidationUserPrompt(taskDescription, textHint, codeHint, userCode),
    temp = 0.0
  )

private val validationHintsCriteria = """
    1. information: Does the hint contain additional information, such as an explanation, tip or compliment? (If the hint contains additional information beyond solving the task, answer with "Yes" and specify what additional information. If it does not, answer with "No".)

    2. levelOfDetail: Is the hint a bottom-out hint ($BOH_KEYWORD) or a high-level description ($HLD_KEYWORD)? (If the hint is a bottom-out hint, answer with "$BOH_KEYWORD". If the hint is a high-level description, answer with "$HLD_KEYWORD".)

    3. personalized: Does the hint correlate with and enhance the student’s current code or approach? (If the hint includes modified student code, improved or enhanced to solve the task, answer with "Yes". If the hint does not correlate with the student's code, answer with "No" and specify why.)

    4. intersection: Is the next step not implemented in the student's code? (If the hint asks to implement something that is not in the student's code, answer with "Yes". If the hint asks to implement something that the student has already implemented, answer with "No" and specify what has already been implemented.)
    
    5. appropriate: Is the hint the correct and relevant next step to solve the problem, based on the task description, the solution steps and given the current state of the student's programme? ? (If the hint is directly working towards the solution of the problem in the task description, and follows the solution steps, given the current state of the student's code and does not ignore errors, answer with "Yes". If the hint is inconsistent with the task's goal, misleads the student, suggests incorrect actions or ignores any errors, answer with "No" and provide an explanation for your decision. An example of an inappropriate hint - a student uses var in his code when the task requires to use val, and the hint does not indicate this error.)
    
    6. specific: Is the hint limited to only one next step? (If the hint is limited to only one next step, answer with "Yes". If the hint contains several steps or the whole solution of the task, answer with "No" and indicate how many steps it contains.)
    
    7. misleadingInformation: Does the hint contain misleading information? (Misleading information includes, but is not limited to, proposing a solution that leads in a different direction, suggesting the implementation of an irrelevant function, or providing information that doesn't align with the task's requirements, or deviating from the solution steps or ignoring errors. If the hint does not contain any misleading information and assists in solving the task correctly, answer with "No". If the hint contains misleading information that might obstruct the student's progress towards the correct solution, answer with "Yes" and specify the misleading information.)
    
    8. codeQuality: Is generated code valid? (If the generated code has no bugs, answer with "Yes", otherwise answer with "No".)
    
    9. kotlinStyle: Is generated code in Kotlin-like style? (If the generated code conforms to common Kotlin language practices and idioms, answer with "Yes", otherwise answer with "No".)
    
    10. length: What is the length of the hint? (Answer with the number of sentences and number of words from the text hint and the number of $NEW_KEYWORD/$CHANGED_KEYWORD/$DELETED_KEYWORD code lines from the code hint.)
  """.trimIndent()

private fun buildHintsValidationUserPrompt(
  taskDescription: String,
  textHint: String,
  codeHint: String,
  userCode: String,
) = """
    Determine the correctness of the hints using the given criteria.
    
    This is a description of the task, representing the goal the student should achieve in their code: <$taskDescription>
    The hints should be designed to guide the student towards fulfilling this task, directly aligning with its objective.
    
    The hint should look at the student's code to determine how the student can proceed with the task.
    
    Current student code: 
      ```kotlin
        $userCode
      ```
    
    Text hint: <$textHint>
    
    Code hint: 
      ```kotlin
        $codeHint
      ```
  """.trimIndent()

private fun buildHintsValidationSystemPrompt() = """
    Text hint and code hint have been generated to guide the student to the next step in solving the task. Your goal is to determine the correctness of these hints using the given criteria as if you were a teacher.
    
    The criteria: <$validationHintsCriteria>
    
    Format the response as json with keys: information, levelOfDetail, personalized, intersection, appropriate, specific, misleadingInformation, codeQuality, kotlinStyle, length.
    
    Find below an example response for reference:
    {
      "information": "No",
      "levelOfDetail": "$HLD_KEYWORD",
      "personalized": "Yes",
      "intersection": "No",
      "appropriate": "No, the student uses var in his code when the task requires to use val, and the hint does not indicate this error",
      "specific": "No, 2 steps",
      "misleadingInformation": "Yes, tries to call a function that does not exist",
      "codeQuality": "Yes",
      "kotlinStyle": "No, can be inlined",
      "length": "the text hint consists of 1 sentence and 12 words, the code hint consists of 1 $NEW_KEYWORD, 3 $CHANGED_KEYWORD, 0 $DELETED_KEYWORD code lines",
    }
  """.trimIndent()

suspend fun processValidationHintForItsType(textHint: String, codeHint: String) =
  grazie.chat(
    llmProfileId = AUTO_VALIDATION_LLM_PROFILE_ID,
    userPrompt = buildFeedbackTypePrompt(textHint, codeHint),
    temp = 0.0
  )

private val feedbackTypeCriterion = """
    Feedback type: What type of feedback is the generated hint? 
    Possible options: (select all the appropriate options)
      a) Knowledge about task constraints:
        i) Hints on task requirements, e.g. a hint that proposes to use a function or a programming construct to solve the task (or don’t use if the task forbids it). (If the hint contains task requirements, add "KTC-TR" to the answer)
        ii) Hints on task-processing rules. These hints provide general information on how to approach the exercise:
          1) TPR General - does not consider the current work of a student. (If the hint contains task-processing rules and does not consider the current work of a student, add "KTC-TPR-TPRG" to the answer)
          2) TPR Context - considers the current work of a student. (If the hint contains task-processing rules and considers the current work of a student, add "KTC-TPR-TPRC" to the answer)
      b) Knowledge about concepts:
        i) Explanations on subject matter, e.g. links to the documentation. (If the hint contains explanations on subject matter, add "KC-EXP" to the answer)
        ii) Examples illustrating concepts, e.g. examples of how to use a concept or a function. (If the hint contains examples illustrating concepts, add "KC-EXA" to the answer)
        iii) Examples that refers to the course content, e.g. refers to task steps or task hints. (If the hint contains examples that refers to the course content, add "KC-ECC" to the answer)
      c) Knowledge about mistakes:
        i) Test failures. (If the hint explains something about test failures, add "KM-TF" to the answer)
        ii) Solution errors, e.g. incorrect algorithm. (If the hint explains something about solution errors, add "KM-SE" to the answer)
        iii) Style issues, e.g. if the hint indicates that style issues should be corrected. (If the hint explains something about style issues, add "KM-SI" to the answer)
        iv) Performance issues. (If the hint explains something about performance issues, add "KM-PI" to the answer)
        v) Language issues, e.g. incorrect language. (If the hint explains something about language issues, add "KM-LI" to the answer)
      d) Knowledge about how to proceed:
        i) Bug-related hints for error correction. Mark a hint with this category if the feedback clearly focuses on what the student should do to correct a mistake:
          1) A hint that may be a in the form of a suggestion, a question, or an example. (If the hint contains a bug-related hint that is in the form of a suggestion, question, or example, add "KH-EC-ECH" to the answer)
          2) A solution that directly shows what needs to be done to correct an error. (If the hint contains a bug-related solution that directly shows what needs to be done to correct an error, add "KH-EC-ECS" to the answer)
          3) Both a hint and a solution. (If the hint contains both a bug-related hint and a bug-related solution, add "KH-EC-ECB" to the answer)
        ii) Task-processing steps. Information about the next step a student has to take to come closer to a solution:
          1) A hint that may be a in the form of a suggestion, a question, or an example. (If the hint contains a hint in the form of a suggestion, question, or example, and that hint includes information about the next step a student has to take to come closer to a solution, add "KH-TPS-TPSH" to the answer)
          2) A solution that directly shows what needs to be done to execute the next step. (If the hint contains a solution that directly shows what needs to be done to execute the next step, add "KH-TPS-TPSS" to the answer)
          3) Both a hint and a solution. (If the hint contains both a hint and a solution that shows what needs to be done to execute the next step, add "KH-TPS-TPSB" to the answer)
""".trimIndent()

private fun buildFeedbackTypePrompt(textHint: String, codeHint: String) = """
    Text hint and code hint have been generated to guide the student to the next step in solving the task. 
    Your goal is to feedback the type of the hint by choosing between the given options: <$feedbackTypeCriterion>
    
    Text hint: <$textHint>
    
    Code hint: 
      ```kotlin
        $codeHint
      ```
    
    Find below an example response for reference: "KTC-TPR-TPRC, KC-EXA, KH-TPS-TPSB"
  """.trimIndent()

suspend fun processValidationCompilationErrorHints(
  textHint: String,
  codeHint: String,
  userCode: String,
  errorDetails: String
) =
  grazie.chat(
    llmProfileId = AUTO_VALIDATION_LLM_PROFILE_ID,
    systemPrompt = buildCompilationErrorHintsValidationSystemPrompt(),
    userPrompt = buildCompilationErrorHintsValidationUserPrompt(textHint, codeHint, userCode, errorDetails),
    temp = 0.0
  )

private val validationCompilationErrorHintsCriteria = """
    1. comprehensible: if the text hint is intelligible (i.e., proper English, not nonsensical), answer with "Yes"; otherwise, answer with "No".
    
    2. unnecessaryContent: if the text hint contains unnecessary content (e.g., repeating content, comprehensible but irrelevant content), answer with "Yes" and indicate unnecessary content; otherwise, answer with "No".
    
    3. hasExplanation: if the text hint contains an explanation of the programming error message, answer with "Yes"; otherwise, answer with "No".
    
    4. explanationCorrect: If the text hint contains a correct explanation of the programming error message, answer with "Yes"; otherwise, answer with "No" and specify why the explanation is not correct.
    
    5. hasFix: If the text hint contains actions or steps that one should take to fix the error, answer with "Yes"; otherwise, answer with "No".
    
    6. fixCorrect: If the text hint contains correct actions or steps that one should take to fix the error, answer with "Yes"; otherwise, answer with "No" and indicate incorrect actions or steps.
    
    7. correctImplementation: if the code hint contains the correct error fix (i.e., actually fixes the error, does not cause other errors, corresponds to the explanation of the fix in the text hint), answer with "Yes"; otherwise, answer with "No" specify why the fix is not correct.
    
    8. improvementOverTheOriginal: if the text hint provides added value (from a novice programmer’s standpoint) when compared to the original programming error message, answer with "Yes" and specify which value; otherwise, answer with "No".
  """.trimIndent()

private fun buildCompilationErrorHintsValidationUserPrompt(
  textHint: String,
  codeHint: String,
  userCode: String,
  errorDetails: String
) = """
    Determine the correctness of the hints using the given criteria.
    
    Original programming error message: <$errorDetails>
    
    Current student code: 
      ```kotlin
        $userCode
      ```
    
    Text hint: <$textHint>
    
    Code hint: 
      ```kotlin
        $codeHint
      ```
  """.trimIndent()

private fun buildCompilationErrorHintsValidationSystemPrompt() = """
    Text hint and code hint have been generated to guide the student to fix a compilation error. Your goal is to determine the correctness of these hints using the given criteria as if you were a teacher.
    
    The criteria: <$validationCompilationErrorHintsCriteria>
    
    Format the response as json with keys: comprehensible, unnecessaryContent, hasExplanation, explanationCorrect, hasFix, fixCorrect, correctImplementation, improvementOverTheOriginal.
    
    Find below an example response for reference:
    {
      "comprehensible": "Yes",
      "unnecessaryContent": "Yes, contains repeating content",
      "hasExplanation": "Yes",
      "explanationCorrect": "No, describes another error",
      "hasFix": "Yes",
      "fixCorrect": "No, actions won't fix the error",
      "correctImplementation": "No, does not fix the error",
      "improvementOverTheOriginal": "Yes, clearer for the novice at the expense of simpler vocabulary",
    }
  """.trimIndent()
