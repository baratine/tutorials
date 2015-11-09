package tutorial.persistence;

public interface CounterServiceSync extends CounterService
{
  long get();

  long addAndGet(long value);
}
