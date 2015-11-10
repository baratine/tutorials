package tutorial.children;

import io.baratine.core.ServiceClient;

public class ClientMain
{
  public static void main(String []args)
    throws Exception
  {
    String url = args[0];

    try (ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build())
    {
      // remote:// is for clients outside a Baratine system
      UserServiceSync service = client.lookup("remote://" + url).as(UserServiceSync.class);

      System.out.println("get() response is: " + service.get());

      System.out.println("set() response is: " + service.set(new User("foo", "bar")));

      System.out.println("get() response is: " + service.get());
    }
  }
}
