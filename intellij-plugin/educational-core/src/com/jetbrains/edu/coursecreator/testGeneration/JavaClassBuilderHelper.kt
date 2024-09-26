package com.jetbrains.edu.coursecreator.testGeneration

//import com.intellij.lang.java.JavaLanguage
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.research.testspark.core.data.TestGenerationData
import java.io.File

object JavaClassBuilderHelper { // TODO replace to java module
  /**
   * Generates the code for a test class.
   *
   * @param className the name of the test class
   * @param body the body of the test class
   * @return the generated code as a string
   */
  fun generateCode(
    project: Project,
    className: String,
    body: String,
    imports: Set<String>,
    packageString: String,
    runWith: String,
    otherInfo: String,
    testGenerationData: TestGenerationData,
  ): String {
    var testFullText = printUpperPart(className, imports, packageString, runWith, otherInfo)

    // Add each test (exclude expected exception)
    testFullText += body

    // close the test class
    testFullText += "}"

    testFullText.replace("\r\n", "\n")

    /**
     * for better readability and make the tests shorter, we reduce the number of line breaks:
     *  when we have three or more sequential \n, reduce it to two.
     */
    return formatJavaCode(project, Regex("\n\n\n(\n)*").replace(testFullText, "\n\n"), testGenerationData)
  }

  /**
   * Returns the upper part of test suite (package name, imports, and test class name) as a string.
   *
   * @return the upper part of test suite (package name, imports, and test class name) as a string.
   */
  private fun printUpperPart(
    className: String,
    imports: Set<String>,
    packageString: String,
    runWith: String,
    otherInfo: String,
  ): String {
    var testText = ""

    // Add package
    if (packageString.isNotBlank()) {
      testText += "package $packageString;\n"
    }

    // add imports
    imports.forEach { importedElement ->
      testText += "$importedElement\n"
    }

    testText += "\n"

    // add runWith if exists
    if (runWith.isNotBlank()) {
      testText += "@RunWith($runWith)\n"
    }
    // open the test class
    testText += "public class $className {\n\n"

    // Add other presets (annotations, non-test functions)
    if (otherInfo.isNotBlank()) {
      testText += otherInfo
    }

    return testText
  }

  /**
   * Finds the test method from a given class with the specified test case name.
   *
   * @param code The code of the class containing test methods.
   * @return The test method as a string, including the "@Test" annotation.
   */
  fun getTestMethodCodeFromClassWithTestCase(code: String): String {
    var result = ""
    try {
      val componentUnit: CompilationUnit = StaticJavaParser.parse(code)
      object : VoidVisitorAdapter<Any?>() {
        override fun visit(method: MethodDeclaration, arg: Any?) {
          super.visit(method, arg)
          if (method.getAnnotationByName("Test").isPresent) {
            result += "\t" + method.toString().replace("\n", "\n\t") + "\n\n"
          }
        }
      }.visit(componentUnit, null)

      return result
    } catch (e: ParseProblemException) {
      val upperCutCode = "\t@Test" + code.split("@Test").last()
      var methodStarted = false
      var balanceOfBrackets = 0
      for (symbol in upperCutCode) {
        result += symbol
        if (symbol == '{') {
          methodStarted = true
          balanceOfBrackets++
        }
        if (symbol == '}') {
          balanceOfBrackets--
        }
        if (methodStarted && balanceOfBrackets == 0) {
          break
        }
      }
      return result + "\n"
    }
  }

  /**
   * Retrieves the name of the test method from a given Java class with test cases.
   *
   * @param oldTestCaseName The old name of test case
   * @param code The source code of the Java class with test cases.
   * @return The name of the test method. If no test method is found, an empty string is returned.
   */
  fun getTestMethodNameFromClassWithTestCase(oldTestCaseName: String, code: String): String {
    var result = ""
    try {
      val componentUnit: CompilationUnit = StaticJavaParser.parse(code)

      object : VoidVisitorAdapter<Any?>() {
        override fun visit(method: MethodDeclaration, arg: Any?) {
          super.visit(method, arg)
          if (method.getAnnotationByName("Test").isPresent) {
            result = method.nameAsString
          }
        }
      }.visit(componentUnit, null)

      return result
    } catch (e: ParseProblemException) {
      return oldTestCaseName
    }
  }

  /**
   * Retrieves the class name from the given test case code.
   *
   * @param code The test case code to extract the class name from.
   * @return The class name extracted from the test case code.
   */
  fun getClassFromTestCaseCode(code: String): String {
    val pattern = Regex("public\\s+class\\s+(\\S+)\\s*\\{")
    val matchResult = pattern.find(code)
    matchResult ?: return "GeneratedTest2321312"
    val (className) = matchResult.destructured
    return className
  }

  /**
   * Formats the given Java code using IntelliJ IDEA's code formatting rules.
   *
   * @param code The Java code to be formatted.
   * @return The formatted Java code.
   */
  fun formatJavaCode(project: Project, code: String, generatedTestData: TestGenerationData): String {
    var result = ""
    WriteCommandAction.runWriteCommandAction(project) {
      val fileName = generatedTestData.resultPath + File.separatorChar + "Formatted.java"
      // create a temporary PsiFile
      val psiFile: PsiFile = PsiFileFactory.getInstance(project)
        .createFileFromText(
          fileName,
          com.intellij.lang.Language.findLanguageByID("JAVA")!!,
          code,
        )

      CodeStyleManager.getInstance(project).reformat(psiFile)

      val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
      result = document?.text ?: code

      File(fileName).delete()
    }

    return result
  }
}
