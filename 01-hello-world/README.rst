Tutorial: Hello World
=====================


Prerequisites
-------------
This tutorial will guide you in building your very first Baratine service.
It assumes that you have already gone through the :doc:`Quick Start <../quick-start>` and have Baratine
running on 127.0.0.1 on the default port of 8085.


Introduction
------------
Baratine is a platform for building POJO microservices.  These services are
loosely-coupled, connecting to one another through POJO interfaces.

At its core, Baratine is very much like an actor system - there is an inbox
queue and a single-threaded worker.  Baratine services are reactive and they
are resilient, responsive, message-driven, and elastic.  They can handle
million of operations per second per instance.

Baratine's value-add proposition is its high abstraction level.  You do not
deal with messages nor forced into a weird abstraction model for your
application.  Baratine lets you build your services around
plain-old-Java-objects (POJOs).  It is true object-oriented programming where
the encapsulation also includes the executing thread.  This allows you to model
your services in lock-step with your business application logic, while
being reactive at the same time.


Definitions
-----------
Inbox: a queue where incoming service requests are serialized into

POJO: plain-old-Java-object, Baratine's first-class abstraction object

Service: a POJO where its interface methods are invokable by clients

Result: asynchronous callback to be executed in the caller's context


Hello World!
------------
Our Hello World service takes in a request and echos it back.  The service
consists of the POJO implementation and the Java interface that clients use
call our service.


HelloServiceImpl.java
^^^^^^^^^^^^^^^^^^^^^
::

     package tutorial.hello;
     
     import io.baratine.core.Result;
     import io.baratine.core.Service;
     
     @Service("public:///hello")
     public class HelloServiceImpl
     {
       public void hello(String msg, Result<String> result)
       {
         result.complete(msg);
       }
     }

Our implementation is just a POJO decorated with ``@Service``.  The annotation
tells Baratine to deploy this service to the ``/hello`` URL endpoint.

The ``hello()`` method includes the ``Result`` callback that our service will
complete and send back to the caller.  The ``Result`` encodes the caller's
address and context.


HelloService.java
^^^^^^^^^^^^^^^^^
::

     package tutorial.hello;
     
     import io.baratine.core.Result;
     
     public interface HelloService
     {
       void hello(String msg, Result<String> result);
     }

The interface is what you distribute to your clients.  Just as with
object-oriented programming, you may distribute different interfaces to
different clients to hide or expose methods.


Deployment
----------
Build the service jar and you can deploy to a running Baratine instance with::

     $ bin/baratine.sh deploy hello.jar

Or you can deploy it as an embedded service programmatically::

     ServerBaratine server = Baratine.newServer()
                                     .port(8085)
                                     .root("file:/tmp/caucho/qa")
                                     .build()
     
     ServiceManager manager = server.newPod("pod")
                                    .build()
                                    .manager();
     
     ServiceRef ref = manager.newService()
                             .address("public:///hello")
                             .service(new HelloServiceImpl())
                             .build();
     
     HelloService service = ref.as(HelloService.class);
     service.hello("world", str -> System.out.println("result is: " + str));
     
     Thread.currentThread().join();
  

The Client
----------
And clients that supports WebSockets or HTTP can talk to our service.  This
includes Java, Python, PHP, JavaScript, and web browsers.  To call our service,
the pattern is the same for all types of clients.  You'll need the:

1. host address (127.0.0.1:8085/s/pod)
2. service URL (/hello)
3. interface class (HelloService.java)

For Java::

     package tutorial.hello;
     
     import io.baratine.core.ServiceClient;
     
     public class HelloClient
     {
       public static void main(String[] args)
         throws Exception
       {
         String message = "world";
   
         if (args.length > 0) {
           message = args[0];
         }
   
         try (ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build()) {
           // remote:// is for clients outside a Baratine system
           HelloService service = client.lookup("remote:///hello").as(HelloService.class);
   
           service.hello(message, str -> System.out.println("result is: " + str));
   
           Thread.sleep(1000 * 5);
         }
       }
     }

The client calls the service as if it's a regular Java method, but
asynchronously.  Asynchronous is the way to go for reactive and high-performance
systems.  However, the client may also choose to call the service synchronously.
The only change is this new method to the interface::

    String hello(String msg);

The POJO implementation stays the same.  Now the client can call it in a
blocking manner::

    String resultStr = service.hello(message);
    System.out.println("Received response: " + resultStr);

Run the client on the command-line and you'll get::

    $ java -cp hello-jar-with-dependencies.jar tutorial.hello.HelloClient "world!"
    Received response: Hello world!


Conclusion
----------
You have just learned how to build a Baratine service and how to call it both
asynchronously and synchronously.  In the :doc:`next tutorial <tutorial-2-batching>`,
we will be building an efficient batching service that takes advantage of
Baratine's implicit batching.
