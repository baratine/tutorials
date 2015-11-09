package tutorial.persistence;

import io.baratine.core.Result;

public interface CounterService
{
  void get(Result<Long> result);

  void addAndGet(long value, Result<Long> result);
}
