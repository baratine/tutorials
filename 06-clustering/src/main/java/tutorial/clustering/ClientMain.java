package tutorial.clustering;

import io.baratine.core.ServiceClient;

public class ClientMain
{
  public static void main(String []args)
    throws Exception
  {
    String id = args[0];

    try (ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/mypod").build())
    {
      UserServiceSync service = client.lookup("remote:///user" + id).as(UserServiceSync.class);

      System.out.println(service.get());

      service.set(new User("first0", "last0"));
      System.out.println(service.get());
    }
  }
}
