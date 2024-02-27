package util

import org.jetbrains.academy.test.system.core.models.variable.TestVariable
import java.io.File

fun TestVariable.variableDefModifier() = "val ${this.name}"

fun TestVariable.variableDefTemplateBase() = "${variableDefModifier()} = ${this.value}"

fun TestVariable.variableDefTemplateWithType() = "${variableDefModifier()}: ${this.javaType} = ${this.value}"

fun TestVariable.isVariableExist(fileContent: String): Boolean {
    val differentStylesWithEqual = listOf("=", " =", "= ", " = ")
    val baseDefs = differentStylesWithEqual.map { "${variableDefModifier()}$it${this.value}" }
    val defWithTypes = listOf(
        "${variableDefModifier()}:${this.javaType}",
        "${variableDefModifier()}: ${this.javaType}",
    ).map { defWithType ->
        differentStylesWithEqual.map{
            "$defWithType$it"
        }
    }.flatten()
    if (!(baseDefs + defWithTypes).any { it in fileContent }) {
        error("The code should contains a definition of the ${this.name} variable! " +
                "Please add <${variableDefTemplateBase()}> or <${variableDefTemplateWithType()}> code in your solution." +
                "Please be careful with styles - check the spaces around =.")
    }
    return true
}

fun checkListOfVariables(sourceCodeFile: File, variables: List<TestVariable>) {
    if (sourceCodeFile.exists()) {
        val content = sourceCodeFile.readText()
        for (variable in variables) {
            assert(variable.isVariableExist(content))
        }
    } else {
        throwInternalCourseError()
    }
}
