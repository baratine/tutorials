package tutorial.hello;

import io.baratine.core.Lookup;
import io.baratine.core.ServiceRef;

import javax.inject.Inject;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {HelloServiceImpl.class}, pod = "pod",
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "WARNING"),
          @ConfigurationBaratine.Log(name = "tutorial", level = "FINER")})
public class HelloServiceTest
{
  @Inject @Lookup("public:///hello")
  private ServiceRef _serviceRef;

  @Inject @Lookup("public:///hello")
  private HelloService _service;

  @Inject
  private RunnerBaratine _testContext;

  @Test
  public void testHello()
  {
    String result = _service.hello("abc");

    assertEquals(result, "hello abc");
  }

  @Test
  public void testRestart()
  {
    testHello();

    restartBaratine();

    testHello();
  }

  private void restartBaratine()
  {
    _testContext.closeImmediate();
    _testContext.start();
  }
}
