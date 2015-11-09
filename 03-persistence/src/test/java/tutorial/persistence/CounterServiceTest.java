package tutorial.persistence;

import javax.inject.Inject;

import io.baratine.core.Lookup;
import tutorial.persistence.CounterServiceImpl;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {CounterServiceImpl.class}, pod = "pod",
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "WARNING"),
          @ConfigurationBaratine.Log(name = "tutorial", level = "FINER")})
public class CounterServiceTest
{
  @Inject @Lookup("public:///counter")
  private CounterServiceSync _service;

  @Inject
  private RunnerBaratine _testContext;

  @Test
  public void testGet()
    throws Exception
  {
    long base = _service.get();

    Assert.assertEquals(base + 111, _service.addAndGet(111));

    Assert.assertEquals(base + 111, _service.get());
  }

  @Test
  public void testAddAndGet()
    throws Exception
  {
    long base = _service.get();

    Assert.assertEquals(base + 111, _service.addAndGet(111));
    Assert.assertEquals(base + 333, _service.addAndGet(222));
    Assert.assertEquals(base + 666, _service.addAndGet(333));
  }

  @Test
  public void testRestart()
    throws Exception
  {
    long base = _service.get();

    Assert.assertEquals(base + 111, _service.addAndGet(111));
    Assert.assertEquals(base + 333, _service.addAndGet(222));
    Assert.assertEquals(base + 666, _service.addAndGet(333));

    //restartBaratine();

    Assert.assertEquals(base + 666, _service.get());
    Assert.assertEquals(base + 777, _service.addAndGet(111));
  }

  private void restartBaratine()
    throws Exception
  {
    _testContext.closeImmediate();
    _testContext.start();
  }
}
