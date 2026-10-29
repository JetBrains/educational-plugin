import org.junit.Assert;
import org.junit.Test;

public class Tests {

  @Test
  public void fail() {
    Assert.fail();
  }

  @Test
  public void test() {
    Assert.assertEquals("Hello", Task.foo());
  }
}
