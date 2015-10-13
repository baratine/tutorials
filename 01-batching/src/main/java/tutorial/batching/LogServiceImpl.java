package tutorial.batching;

import io.baratine.core.AfterBatch;
import io.baratine.core.BeforeBatch;
import io.baratine.core.OnDestroy;
import io.baratine.core.OnInit;
import io.baratine.core.Result;
import io.baratine.core.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

@Service("public:///log")
public class LogServiceImpl
{
  private PrintStream _out;

  public void log(String msg, Result<Void> result)
  {
    _out.println(msg);

    result.complete(null);
  }

  @OnInit
  public void onInit(Result<Void> result)
  {
    try {
      boolean isAutoFlush = false;

      _out = new PrintStream(new FileOutputStream("/tmp/tutorial_log.txt", true), isAutoFlush);

      result.complete(null);
    }
    catch (IOException e) {
      // catching exceptions is not required because Baratine will automatically
      // call fail() for uncaught exceptions
      result.fail(e);
    }
  }

  @BeforeBatch
  public void beforeBatch()
  {
    // nothing to do here
  }

  @AfterBatch
  public void afterBatch()
  {
    _out.flush();
  }

  @OnDestroy
  public void onShutdown(Result<Void> result)
  {
    _out.close();
    _out = null;

    result.complete(null);
  }
}
