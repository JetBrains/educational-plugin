import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class Tests {
  @Test
  public void test() {
    Assert.assertTrue("Task.foo() should return 42", Task.foo() == 42);
  }
  @Test
  @Ignore
  public void ignoredTest() {
    Assert.assertTrue("Task.foo() should return 42", Task.foo() == 43);
  }
}
