package util

import org.jetbrains.academy.test.system.core.findMethod
import org.jetbrains.academy.test.system.core.invokeWithArgs
import org.jetbrains.academy.test.system.core.invokeWithoutArgs
import org.jetbrains.academy.test.system.core.models.method.TestMethod
import org.junit.jupiter.api.Assertions
import util.Util.newLineSeparator
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

object Util {
    const val DEFAULT_USER_INPUT = "<a user's answer>"

    val newLineSeparator: String = System.lineSeparator()
}

fun setSystemIn(input: List<String>? = null) = setSystemIn(input?.joinToString(newLineSeparator))

fun setSystemIn(input: String? = null) = input?.let {
    System.setIn(it.replaceLineSeparator().byteInputStream())
}

fun String.replaceLineSeparator() = this.lines().joinToString(newLineSeparator)

fun setSystemOut(): ByteArrayOutputStream {
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos, true, StandardCharsets.UTF_8.name())
    System.setOut(ps)
    return baos
}

@Suppress("SwallowedException")
fun runMainFunction(mainFunction: () -> Unit, input: String? = null, toAssertSystemIn: Boolean = true): String {
    return try {
        setSystemIn(input)
        val baos = setSystemOut()
        mainFunction()
        if (toAssertSystemIn) {
            Assertions.assertTrue(isSystemInEmpty()) { "You are asking the user to enter data fewer times than required in the task!" }
        }
        baos.toString("UTF-8").replaceLineSeparator()
    } catch (e: IllegalStateException) {
        val userInput = input?.let { "the user input: $it" } ?: "the empty user input"
        val errorPrefix =
            "Try to run the main function with $userInput, the function must process the input and exit, but the current version of the function"
        if ("Your input is incorrect" in (e.message ?: "")) {
            Assertions.assertTrue(false) { "$errorPrefix waits more user inputs, it must be fixed." }
        }
        Assertions.assertTrue(false) { "$errorPrefix throws an unexpected error, please, check the function's implementation." }
        ""
    } catch (e: NotImplementedError) {
        Assertions.assertTrue(false) { "You call not implemented functions (that use TODO()) inside the main function. Please, don't do it until the task asks it" }
        ""
    }
}

fun isSystemInEmpty() = String(System.`in`.readBytes()).isEmpty()

fun throwInternalCourseError(): Nothing = error("Internal course error!")

@Suppress("LongParameterList")
fun checkReadLineFunctions(
    vararg args: Any,
    testMethod: TestMethod,
    clazz: Class<*>,
    input: String,
    isSystemInEmpty: Boolean,
    output: String,
) {
    val userMethod = clazz.methods.findMethod(testMethod)
    setSystemIn(input)
    val result =
        if (args.isEmpty()) userMethod.invokeWithoutArgs(clazz) else userMethod.invokeWithArgs(*args, clazz = clazz)
    val errorPostfix = if (!isSystemInEmpty) "not" else ""
    Assertions.assertEquals(
        isSystemInEmpty, isSystemInEmpty(),
        "For the user's input: $input the function ${testMethod.name} should read $errorPostfix " +
                "all inputs before returning the result."
    )
    Assertions.assertEquals(
        output, result, "For the user's input: $input the " +
                "function ${testMethod.name} should return $output"
    )
}
