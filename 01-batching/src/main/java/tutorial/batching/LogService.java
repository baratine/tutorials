package tutorial.batching;

import io.baratine.core.Result;

public interface LogService
{
  void log(String msg, Result<Void> result);

  void log(String msg);
}
