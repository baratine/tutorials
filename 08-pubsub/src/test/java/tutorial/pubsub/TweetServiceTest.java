package tutorial.pubsub;

import javax.inject.Inject;

import io.baratine.core.Lookup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {TweetServiceImpl.class}, pod = "pod",
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "WARNING"),
          @ConfigurationBaratine.Log(name = "tutorial", level = "FINER")})
public class TweetServiceTest
{
  @Inject @Lookup("public:///tweet")
  private TweetService _service;

  @Inject
  private RunnerBaratine _testContext;

  //@Test
  public void testGet()
    throws Exception
  {
    _service.subscribe(System.out::println);

    _service.publish("hello world");
  }

  @Test
  public void testRestart()
    throws Exception
  {



    //restartBaratine();


  }

  private void restartBaratine()
    throws Exception
  {
    _testContext.closeImmediate();
    _testContext.start();
  }
}
