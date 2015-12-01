package tutorial.clustering;

public interface UserServiceSync extends UserService
{
  User get();

  boolean set(User user);
}
