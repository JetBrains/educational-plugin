import org.junit.Assert;

public class Test {
  @org.junit.Test
  public void test() {
    Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
  }
}
