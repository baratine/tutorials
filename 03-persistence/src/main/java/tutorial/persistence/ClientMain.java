package tutorial.persistence;

import io.baratine.core.ServiceClient;

public class ClientMain
{
  public static void main(String []args)
    throws Exception
  {
    long value = args.length > 0 ? Long.parseLong(args[0]) : 111;

    try (ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build())
    {
      // remote:// is for clients outside a Baratine system
      CounterServiceSync service = client.lookup("remote:///counter").as(CounterServiceSync.class);

      System.out.println("Response is: " + service.addAndGet(value));
    }
  }
}
