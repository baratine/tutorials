package tutorial.batching;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import io.baratine.core.Lookup;
import io.baratine.core.ResultFuture;
import io.baratine.core.ServiceRef;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.caucho.junit.ConfigurationBaratine;
import com.caucho.junit.RunnerBaratine;

@RunWith(RunnerBaratine.class)
@ConfigurationBaratine(services = {LogServiceImpl.class}, pod = "pod",
  logs = {@ConfigurationBaratine.Log(name = "com.caucho", level = "WARNING"),
          @ConfigurationBaratine.Log(name = "tutorial", level = "FINER")})
public class LogServiceTest
{
  @Inject @Lookup("public:///log")
  private ServiceRef _serviceRef;

  @Inject @Lookup("public:///log")
  private LogService _service;

  @Inject
  private RunnerBaratine _testContext;

  @BeforeClass
  @AfterClass
  public static void cleanupStatic()
    throws Exception
  {
    Path path = FileSystems.getDefault().getPath("/tmp/tutorial_log.txt");
    Files.deleteIfExists(path);
  }

  @Before
  public void cleanup()
    throws Exception
  {
    _testContext.closeImmediate();

    cleanupStatic();

    _testContext.start();
  }

  @Test
  public void testLog()
    throws Exception
  {
    ResultFuture<Void> future = new ResultFuture<>();

    _service.log("aaa");
    _service.log("bbb");
    _service.log("ccc", future);
    future.get();

    try (BufferedReader reader = new BufferedReader(new FileReader("/tmp/tutorial_log.txt"))) {
      Assert.assertEquals("aaa", reader.readLine());
      Assert.assertEquals("bbb", reader.readLine());
      Assert.assertEquals("ccc", reader.readLine());

      Assert.assertEquals(null, reader.readLine());
    }
  }

  @Test
  public void testRestart()
    throws Exception
  {
    ResultFuture<Void> future = new ResultFuture<>();

    _service.log("aaa");
    _service.log("bbb");
    _service.log("ccc", future);
    future.get();

    restartBaratine();
    future = new ResultFuture<>();

    _service.log("ddd");
    _service.log("eee");
    _service.log("fff", future);
    future.get();

    try (BufferedReader reader = new BufferedReader(new FileReader("/tmp/tutorial_log.txt"))) {
      Assert.assertEquals("aaa", reader.readLine());
      Assert.assertEquals("bbb", reader.readLine());
      Assert.assertEquals("ccc", reader.readLine());
      Assert.assertEquals("ddd", reader.readLine());
      Assert.assertEquals("eee", reader.readLine());
      Assert.assertEquals("fff", reader.readLine());

      Assert.assertEquals(null, reader.readLine());
    }
  }

  private void restartBaratine()
    throws Exception
  {
    _testContext.closeImmediate();
    _testContext.start();
  }
}
