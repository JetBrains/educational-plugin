package com.jetbrains.edu.commandLine.validation

import com.jetbrains.edu.coursecreator.validation.ValidationSuite
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class JsonValidationResultConsumer(outputConsumer: ValidationOutputConsumer) : ValidationResultConsumer(outputConsumer) {

  @OptIn(ExperimentalSerializationApi::class)
  override fun consume(rootNode: ValidationSuite) {
    val json = Json {
      prettyPrint = true
      prettyPrintIndent = "  "
    }

    val jsonString = json.encodeToString(rootNode)
    outputConsumer.consume(jsonString)
  }
}
