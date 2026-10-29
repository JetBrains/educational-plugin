import org.junit.Assert
import org.junit.Test

class CustomTests {
    @Test
    fun fail() {
        Assert.fail()
    }

    @Test
    fun test() {
        Assert.assertEquals("Hello!", bar())
    }
}
