import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite

@RunWith(classOf[JUnitRunner])
class TestSpec extends FunSuite {
  test("Test") {
    assertResult(42) { new Task().foo() }
  }
}
