package tutorial.pubsub;

import io.baratine.core.ResultStream;
import io.baratine.core.ServiceClient;

public class TestMain
{
  public static void main(String[] args)
    throws Exception
  {
    try (ServiceClient client = ServiceClient.newClient("ws://127.0.0.1:8085/s/pod").build())
    {
      // remote:// is for clients outside a Baratine system
      TweetService service = client.lookup("remote:///tweet").as(TweetService.class);

      /*
      service.search(args[0], phrase -> {
        System.out.println("received phrase: " + phrase);
      });
      */

      System.out.println("TestMain0");

      service.subscribe(new ResultStream<String>() {
        public void accept(String value)
        {
          System.out.println("TestMain10: " + value);
        }

        public void complete()
        {
          System.out.println("TestMain11 completed");
        }

        public void fail(Throwable t)
        {
          System.out.println("TestMain12 exception: " + t);

          t.printStackTrace();
        }
      });

      System.out.println("TestMain1");

      Thread.currentThread().join();

      System.out.println("TestMain2");
    }

    System.out.println("TestMain9");
  }
}
