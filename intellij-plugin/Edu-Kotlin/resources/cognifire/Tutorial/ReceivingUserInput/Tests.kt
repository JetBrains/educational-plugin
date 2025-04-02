import org.jetbrains.academy.test.system.kotlin.test.BaseIjTestClass
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File


class Test : BaseIjTestClass() {
    companion object {

        private lateinit var sourceText: String

        @JvmStatic
        @BeforeAll
        fun initialize() {
            val taskDirectoryPath = System.getProperty("user.dir")
            val sourceCodeFile =
                File("$taskDirectoryPath/src/jetbrains/course/tutorial/Main.kt")
            sourceText = sourceCodeFile.readText()
        }
    }

    @Test
    fun testHumanYearsInputVariable()() {
        setUp()
        myFixture.configureByText("Task.kt", sourceText)

        val argument = getMethodCallArguments("prompt")?.firstOrNull()
        Assertions.assertNotNull(argument) {
            "Please, create a prompt block."
        }
        Assertions.assertTrue(argument?.contains("humanYearsInput") == true) {
            "Please, save the age into a variable named humanYearsInput."
        }
    }

    @Test
    fun testBackticks() {
        setUp()
        myFixture.configureByText("Task.kt", sourceText)

        val argument = getMethodCallArguments("prompt")?.firstOrNull()
        Assertions.assertNotNull(argument) {
            "Please, create a prompt block."
        }
        Assertions.assertNotNull(Regex("`[^`]*`").findAll(argument!!).map { it.value }.find { it == "`humanYearsInput`" }) {
            "Please, wrap all identifiers in backticks (``)."
        }
    }
}
