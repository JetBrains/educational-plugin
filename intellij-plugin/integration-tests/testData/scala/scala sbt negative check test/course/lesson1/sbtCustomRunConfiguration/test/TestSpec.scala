import org.scalatest.FunSuite

class TestSpec extends FunSuite {
  test("Test") {
    assertResult("Hello") { new Task().foo() }
  }
}
