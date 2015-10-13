package tutorial.batching;

import io.baratine.core.ServiceManager;
import io.baratine.core.ServiceRef;

import com.caucho.baratine.Baratine;
import com.caucho.baratine.embed.ServerBaratine;

public class Main
{
  public static void main(String[] args)
    throws Exception
  {
    try (ServerBaratine server = Baratine.newServer()
                                         .port(8085)
                                         .root("file:/tmp/caucho/qa")
                                         .build())
    {
      ServiceManager manager = server.newPod("pod")
                                     .build()
                                     .manager();

      ServiceRef ref = manager.newService()
                              .address("public:///log")
                              .service(new LogServiceImpl())
                              .build();

      LogService service = ref.as(LogService.class);

      service.log("logme timbers");

      Thread.currentThread().join();
    }
  }
}
