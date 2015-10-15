package tutorial.hello;

import com.caucho.baratine.client.BaratineClient;

public class HelloClient
{
  public static void main(String []args)
    throws Exception
  {
    String message = "world";

    if (args.length > 0) {
      message = args[0];
    }

    try (BaratineClient client = new BaratineClient("http://127.0.0.1:8085/s/pod"))
    {
      // remote:// is for clients outside a Baratine system
      HelloService service = client.lookup("remote:///hello").as(HelloService.class);

      System.out.println("first result is: " + service.hello(message));
      service.hello(message, str -> System.out.println("second result is: " + str));

      Thread.sleep(1000 * 5);
    }
  }
}
