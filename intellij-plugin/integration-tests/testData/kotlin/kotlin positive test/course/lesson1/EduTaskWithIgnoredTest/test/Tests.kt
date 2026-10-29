import org.junit.Assert
import org.junit.Ignore
import org.junit.Test

class Test {
    @Test
    fun testSolution() {
        Assert.assertTrue("foo() should return 42", foo() == 42)
    }

    @Test
    @Ignore
    fun ignoredTest() {
        Assert.assertTrue("foo() should return 42", foo() == 43)
    }
}
