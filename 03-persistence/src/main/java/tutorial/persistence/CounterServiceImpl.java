package tutorial.persistence;

import javax.inject.Inject;

import io.baratine.core.Journal;
import io.baratine.core.Lookup;
import io.baratine.core.Modify;
import io.baratine.core.OnActive;
import io.baratine.core.OnLoad;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.store.Store;

@Journal
@Service("public:///counter")
public class CounterServiceImpl
{
  private long _count;

  @Inject @Lookup("store:///counter")
  private Store<Long> _store;

  public void get(Result<Long> result)
  {
    result.complete(_count);
  }

  @Modify
  public void addAndGet(long value, Result<Long> result)
  {
    _count += value;

    result.complete(_count);
  }

  @OnLoad
  public void onLoad(Result<Void> result)
  {
    _store.get("count", result.from(value -> onLoadComplete(value)));
  }

  private Void onLoadComplete(Long value)
  {
    _count = value != null ? value : 0L;

    return null;
  }

  @OnSave
  public void onSave(Result<Void> result)
  {
    _store.put("count", _count, result);
  }

  @OnActive
  public void onActive(Result<Void> result)
  {
    System.out.println("counter service is now active");

    result.complete(null);
  }
}