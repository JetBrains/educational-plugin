import org.scalatest.FunSuite

class TestSpec extends FunSuite {
  test("Test") {
    assertResult(42) { new Task().foo() }
  }
}
