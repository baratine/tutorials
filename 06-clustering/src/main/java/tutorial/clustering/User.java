package tutorial.clustering;

import java.io.Serializable;

public class User implements Serializable
{
  private String _first;
  private String _last;

  public User(String first, String last)
  {
    _first = first;
    _last = last;
  }

  public String getFirst()
  {
    return _first;
  }

  public String getLast()
  {
    return _last;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _first + " " + _last + "]";
  }
}
