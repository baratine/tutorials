package tutorial.hello;

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
                              .address("public:///hello")
                              .service(new HelloServiceImpl())
                              .build();

      HelloService service = ref.as(HelloService.class);

      System.out.println("first result is: " + service.hello("world sync"));
      service.hello("world", str -> System.out.println("second result is: " + str));

      Thread.currentThread().join();
    }
  }
}
