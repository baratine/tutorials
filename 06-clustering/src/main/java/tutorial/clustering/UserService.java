package tutorial.clustering;

import io.baratine.core.Result;

public interface UserService
{
  void get(Result<User> result);

  void set(User user, Result<Boolean> result);
}
