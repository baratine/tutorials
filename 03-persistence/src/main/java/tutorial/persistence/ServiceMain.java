package tutorial.persistence;

import com.caucho.v5.baratine.Baratine;
import com.caucho.v5.baratine.embed.ServerBaratine;

import io.baratine.core.ServiceManager;
import io.baratine.core.ServiceRef;

public class ServiceMain
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
                              .address("public:///counter")
                              .service(new CounterServiceImpl())
                              .build();

      CounterServiceSync service = ref.as(CounterServiceSync.class);

      System.out.println("result is: " + service.addAndGet(111));

      Thread.currentThread().join();
    }
  }
}
