package tutorial.batching;

import com.caucho.baratine.client.BaratineClient;

public class LogClient
{
  public static void main(String []args)
    throws Exception
  {
    String message = "logme timbers";

    if (args.length > 0) {
      message = args[0];
    }

    try (BaratineClient client = new BaratineClient("http://127.0.0.1:8085/s/pod"))
    {
      // remote:// is for clients outside a Baratine system
      LogService service = client.lookup("remote:///log").as(LogService.class);

      service.log(message);

      Thread.sleep(1000 * 5);
    }
  }
}
