package tutorial.pubsub;

import io.baratine.core.ResultStream;
import io.baratine.core.ServiceClient;
import io.baratine.core.ServiceExceptionClosed;

public class ClientMain
{
  public static void main(String []args)
    throws Exception
  {
    try (ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build())
    {
      TweetService service = client.lookup("remote:///tweet").as(TweetService.class);

      service.subscribe(new MyResultStream(service));

      Thread.sleep(1000);

      service.publish("hello world");

      Thread.currentThread().join();
    }
  }

  static class MyResultStream implements ResultStream<String> {
    private TweetService _service;

    public MyResultStream(TweetService service)
    {
      _service = service;
    }

    public void accept(String value)
    {
      System.out.println(getClass().getSimpleName() + ".accept: " + value);
    }

    public void complete()
    {
      System.out.println(getClass().getSimpleName() + ".complete");
    }

    public void fail(Throwable e)
    {
      System.out.println(getClass().getSimpleName() + ".fail: " + e);

      if (e instanceof ServiceExceptionClosed) {
        System.out.println(getClass().getSimpleName() + ".fail: timed out, reconnecting");

        _service.subscribe(new MyResultStream(_service));
      }
    }
  }
}
