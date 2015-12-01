package tutorial.clustering;

import javax.inject.Inject;

import io.baratine.core.Lookup;
import io.baratine.core.ServiceRef;
import tutorial.clustering.User;
import tutorial.clustering.UserServiceImpl;
import tutorial.clustering.UserServiceSync;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {UserServiceImpl.class}, pod = "pod",
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "WARNING"),
          @ConfigurationBaratine.Log(name = "tutorial", level = "FINER")})
public class UserServiceTest
{
  @Inject @Lookup("public:///user")
  private ServiceRef _baseRef;

  @Inject
  private RunnerBaratine _testContext;

  //@Test
  public void testGet()
    throws Exception
  {
    UserServiceSync service = _baseRef.lookup("/testGet").as(UserServiceSync.class);

    Assert.assertEquals(null, service.get());

    User user = new User("first0", "last0");

    Assert.assertEquals(true, service.set(user));
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
