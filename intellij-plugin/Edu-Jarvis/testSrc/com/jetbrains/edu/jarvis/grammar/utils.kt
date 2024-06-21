package com.jetbrains.edu.jarvis.grammar

import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.callSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.declareSynonyms
import com.jetbrains.edu.jarvis.DescriptionErrorAnnotator.Companion.functionSynonyms

const val NUMBER_OF_RUNS = 10
const val MIN_IDENTIFIER_NAME_LENGTH = 4
const val MAX_IDENTIFIER_NAME_LENGTH = 30

const val MIN_NUMBER_OF_ARGS = 0
const val MAX_NUMBER_OF_ARGS = 10

const val EMPTY_STRING = ""

const val OPEN_PARENTHESIS = "("
const val CLOSED_PARENTHESIS = ")"
const val ARGUMENT_SEPARATOR = ","

const val THE = "the"

data class TestAnswer(val input: String, val answer: Any)

val identifierFirstCharacter = ('a'..'z') + ('A'..'Z') + '_'
val identifierOtherCharacter = ('0'..'9')

fun generateNamedFunction(functionNameLength: Int, arguments: Collection<String>) =
  "${generateIdentifier(functionNameLength)}$OPEN_PARENTHESIS${arguments.joinToString(ARGUMENT_SEPARATOR)}$CLOSED_PARENTHESIS"

fun generateIdentifier(identifierNameLength: Int) =
  (1..identifierNameLength).map {
    if(it == 1) (identifierFirstCharacter).random()
    else (identifierFirstCharacter + identifierOtherCharacter).random()
  }.joinToString(EMPTY_STRING)

fun generateNoParenthesesFunction(functionNameLength: Int): TestAnswer {
  val functionName = generateIdentifier(functionNameLength)
  return TestAnswer(
    "${callSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()}" +
    " ${(listOf(EMPTY_STRING) + functionSynonyms()).random()} `$functionName`",
    functionName
  )
}

fun generateVariableDeclaration(variableNameLength: Int): TestAnswer {
  val variableName = generateIdentifier(variableNameLength)
  return TestAnswer(
    "${declareSynonyms().random()} ${listOf(EMPTY_STRING, THE).random()} `${variableName}`",
    variableName
  )
}
