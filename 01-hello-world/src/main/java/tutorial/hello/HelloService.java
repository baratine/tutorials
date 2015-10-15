package tutorial.hello;

import io.baratine.core.Result;

public interface HelloService
{
  void hello(String msg, Result<String> result);

  String hello(String msg);
}