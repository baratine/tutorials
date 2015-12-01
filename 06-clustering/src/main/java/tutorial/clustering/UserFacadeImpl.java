package tutorial.clustering;

import javax.inject.Inject;

import io.baratine.core.Lookup;
import io.baratine.core.OnLookup;
import io.baratine.core.Result;
import io.baratine.core.Service;
import io.baratine.core.ServiceRef;

@Service("public:///user")
public class UserFacadeImpl
{
  @Inject @Lookup("pod://mypod/_user")
  private ServiceRef _ref;

  @OnLookup
  public UserFacadeChild onLookup(String url)
  {
    UserService service = _ref.lookup(url).as(UserService.class);

    return new UserFacadeChild(service);
  }

  static class UserFacadeChild implements UserService
  {
    private UserService _service;

    public UserFacadeChild(UserService service)
    {
      _service = service;
    }

    public void get(Result<User> result)
    {
      _service.get(result);
    }

    public void set(User user, Result<Boolean> result)
    {
      _service.set(user, result);
    }
  }
}
