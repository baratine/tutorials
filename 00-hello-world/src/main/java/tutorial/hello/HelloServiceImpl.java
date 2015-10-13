package tutorial.hello;

import io.baratine.core.Result;
import io.baratine.core.Service;

@Service("public:///hello")
public class HelloServiceImpl
{
  public void hello(String msg, Result<String> result)
  {
    result.complete("hello " + msg);
  }
}
