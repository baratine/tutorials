package tutorial.children;

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
@Service("public:///user")
public class UserServiceImpl
{
  private User _user;

  @Inject @Lookup("store:///user")
  private Store<User> _store;

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

  @OnActive
  public void onActive(Result<Void> result)
  {
    System.out.println("user service is now active");

    result.complete(null);
  }
}