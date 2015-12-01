package tutorial.clustering;

import javax.inject.Inject;

import io.baratine.core.Journal;
import io.baratine.core.Lookup;
import io.baratine.core.OnActive;
import io.baratine.core.OnLookup;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceRef;
import io.baratine.store.Store;

@Journal
@Service("pod://mypod/_user")
public class UserManagerImpl
{
  @Inject @Lookup("store:///user")
  private ServiceRef _storeRef;

  @OnLookup
  public UserServiceImpl onLookup(String url)
  {
    // give the child its partition of the Store
    Store<User> store = _storeRef.lookup(url).as(Store.class);

    return new UserServiceImpl(store);
  }

  @OnActive
  public void onActive(Result<Void> result)
  {
    System.out.println("user service is now active");

    result.complete(null);
  }
}
