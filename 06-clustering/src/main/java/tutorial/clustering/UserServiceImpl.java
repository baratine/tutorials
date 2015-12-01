package tutorial.clustering;

import io.baratine.core.Modify;
import io.baratine.core.OnLoad;
import io.baratine.core.OnSave;
import io.baratine.core.Result;
import io.baratine.store.Store;

public class UserServiceImpl
{
  private User _user;
  private Store<User> _store;

  public UserServiceImpl(Store<User> store)
  {
    _store = store;
  }

  public void get(Result<User> result)
  {
    result.complete(_user);
  }

  @Modify
  public void set(User user, Result<Boolean> result)
  {
    _user = user;

    result.complete(true);
  }

  @OnLoad
  public void onLoad(Result<Void> result)
  {
    _store.get("user", result.from(value -> onLoadComplete(value)));
  }

  private Void onLoadComplete(User user)
  {
    _user = user;

    return null;
  }

  @OnSave
  public void onSave(Result<Void> result)
  {
    if (_user != null) {
      _store.put("user", _user, result);
    }
    else {
      _store.remove("user", result);
    }
  }
}
