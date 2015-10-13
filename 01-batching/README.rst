Tutorial: Batching
==================


Prerequisites
-------------
This tutorial will show you how to use Baratine's batching hooks to
easily take advantage of Baratine's built-in batching support.  It assumes you
already know how to build and call a Baratine service.


Introduction
------------
One of Baratine's core features is its automatic and implicit batching.
Baratine will automatically batch requests and responses with batch sizes
being dependent on load.  A lightly-loaded system will usually have a batch size
of 1, whereas a heavily-loaded system will have a batch size dependent on load
and back pressure at that particular point in time.

Batching is implicit, meaning that your service deals with a method call at a
time.  However, you may hook your service into Baratine's batching life-cycle
events to do an expensive operation once per batch (instead of on every method
call).  Logging is a good example to batch because often times it becomes a
bottleneck in by itself.


The Batching Logging Service
----------------------------
Baratine provides a variety of lifecycle annotation hooks on methods.  For now,
we'll introduce ``@OnInit``, ``@OnDestroy``, ``@BeforeBatch``, and
``@AfterBatch``.


LogServiceImpl.java
^^^^^^^^^^^^^^^^^^^
::

     package tutorial.batching;
   
     import io.baratine.core.AfterBatch;
     import io.baratine.core.BeforeBatch;
     import io.baratine.core.OnDestroy;
     import io.baratine.core.OnInit;
     import io.baratine.core.Result;
     import io.baratine.core.Service;
   
     import java.io.FileOutputStream;
     import java.io.IOException;
     import java.io.PrintStream;
   
     @Service("public:///log")
     public class LogServiceImpl
     {
       private PrintStream _out;
   
       public void log(String msg, Result<Void> result)
       {
         _out.println(msg);
   
         result.complete(null);
       }
   
       @OnInit
       public void onInit(Result<Void> result)
       {
         try {
           boolean isAutoFlush = false;
   
           _out = new PrintStream(new FileOutputStream("/tmp/tutorial_log.txt", true), isAutoFlush);
   
           result.complete(null);
         }
         catch (IOException e) {
           // catching exceptions is not required because Baratine will automatically
           // call fail() for uncaught exceptions
           result.fail(e);
         }
       }
   
       @BeforeBatch
       public void beforeBatch()
       {
         // nothing to do here
       }
   
       @AfterBatch
       public void afterBatch()
       {
         _out.flush();
       }
   
       @OnDestroy
       public void onShutdown(Result<Void> result)
       {
         _out.close();
         _out = null;
   
         result.complete(null);
       }
     }

We're using ``@OnInit`` to open the stream without automatic line flushing.
This minimizes the chances that our ``log()`` method will block from calling
``PrintStream.println()``.  ``@OnDestroy`` closes the stream.  ``@AfterBatch``
flushes the stream to disk, which is a very expensive and slow operation.  We
do it once per batch to reduce load on the disk, reduce service pauses, and to
improve overall service responsiveness.

A life-cycle method can accept a ``Result`` argument.  You may complete it with
any value of any type at a later time, just as long as it is completed to
signal to Baratine that you are done.


LogService.java
^^^^^^^^^^^^^^^
::

     package example.logging;
     
     public interface LogService
     {
       public void log(String msg, Result<Void> result);
     }

The above is the interface that Java clients will use to call our service.  If
you want ``log()`` to be fire-and-forgot, in other words, you do not care about
the response, then just remove ``Result`` from the interface method::

    public void log(String msg);

Baratine will not bother sending the response back to the client.


Deployment
----------
Build the service jar and you can deploy to a running Baratine instance with::

     $ bin/baratine.sh deploy batching.jar

Or you can deploy it as an embedded service programmatically::

     ServerBaratine server = Baratine.newServer()
                                     .port(8085)
                                     .root("file:/tmp/caucho/qa")
                                     .build()
     
     ServiceManager manager = server.newPod("pod")
                                    .build()
                                    .manager();
     
     ServiceRef ref = manager.newService()
                             .address("public:///log")
                             .service(new LogServiceImpl())
                             .build();
     
     Thread.currentThread().join();


The Client
----------
::

    BaratineClient client = new BaratineClient("http://127.0.0.1:8085/s/pod");
    
    LogService service = client.lookup("remote:///log").as(LogService.class);
    
    // fire-and-forget
    service.log("logme timbers");


Conclusion
----------
You have just learned how to use Baratine's life-cycle annotations to easily
implement batching.  The :doc:`next tutorial <tutorial-3-persistence>` will cover persistence.
