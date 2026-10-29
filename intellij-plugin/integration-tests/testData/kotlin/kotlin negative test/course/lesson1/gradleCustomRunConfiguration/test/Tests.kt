import org.junit.Assert
import org.junit.Test

class Tests {
    @Test
    fun fail() {
        Assert.fail()
    }

    @Test
    fun testSolution() {
        Assert.assertEquals("Hello", foo())
    }
}
