package tutorial.pubsub;

import com.caucho.v5.baratine.Baratine;
import com.caucho.v5.baratine.ServiceServer;

import io.baratine.core.ServiceManager;
import io.baratine.core.ServiceRef;

public class ServiceMain
{
  public static void main(String[] args)
    throws Exception
  {
    ServiceServer server = Baratine.newServer()
                                        .port(8085)
                                        .root("file:/tmp/baratine")
                                        .build();

    ServiceManager manager = server.newPod("pod")
                                   .build()
                                   .manager();

    ServiceRef ref = manager.newService()
                            .address("public:///tweet")
                            .service(new TweetServiceImpl())
                            .build();

    TweetService service = ref.as(TweetService.class);

    Thread.currentThread().join();
  }
}
