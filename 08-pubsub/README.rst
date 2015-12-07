.. _examples-pubsub:

Tutorial: Publish/Subscribe
===========================


Prerequisites
-------------
This example will show you how to build a publish/subscribe service for Java
and JavaScript clients.


Introduction
------------
A large subset of applications work better with the publish/subscribe model
than the RPC model.  In publish/subscribe, a client needs to only register once
with a service and it will continually receive multiple responses.  This model
is one that Baratine natively supports.


GitHub Repository
-----------------
The code for this tutorial is in `GitHub <https://github.com/baratine/tutorials/tree/master/08-pubsub>`_.


The ResultStream
----------------
Central to Baratine's publish/subscribe support is the
``io.baratine.core.ResultStream``.  Whereas a ``Result`` may only accept one
return value, the ``ResultStream`` may accept many values.  Like ``Result``, it
is just an argument to your service method::

    public void subscribe(ResultStream<String> result)
    {
      result.accept("aaa");
      result.accept("bbb");
      result.accept("ccc");
      
      result.complete();
    }

A Java client can call your this method by passing in its ``ResultStream``::

    myService.subscribe(new ResultStream<String>() {
      public void accept(String value)
      {
        System.out.println("accept: " + value);
      }
      
      public void complete()
      {
        System.out.println("complete");
      }
      
      public void fail(Throwable t)
      {
        System.out.println("fail: " + t);
        t.printStackTrace();
      }
    });
 
The service sends 4 messages to the client: "aaa", "bbb", "ccc", and the
`complete` message.  The client receives those messages and executes its
``accept()`` method 3 times, and finally its ``complete()`` method.
 
You may use ``ResultStream`` for streaming, hence the name, and
publish/subscribe applications.


JavaScript Clients
------------------
Streaming (i.e. service methods with ``ResultStream``) is not restricted to just
Java clients; it is also accessible by non-Java clients using WebSocket with
HTTP long-polling fallback.

For JavaScript, we provide a client library on
`GitHub <https://github.com/baratine/baratine-js>` that makes calling Baratine
services as easy as possible.  Here is the JavaScript to call the same service
above::

    <html>
      <head>
        <!-- available at
          https://raw.githubusercontent.com/baratine/baratine-js/master/dist/baratine-js.js
        -->
        <script type="text/javascript" src="baratine-js.js"></script>
      </head>
      
      <body>
        <script type="text/javascript">
          var client = new Jamp.BaratineClient("ws://localhost:8085/s/pod");
          
          var callback = function(value) {
            console.log("accept: " + value);
          };
          
          callback.onfail = function(error) {
            console.log("fail: " + error);
          };
          
          callback.oncomplete = function() {
            console.log("complete");
          };
          
          client.stream("/myService", "subscribe", [], callback);
        </script>
      </body>
    
    </html


The Tweet Service
-----------------
Here is the full application for publishing nessages to a wide variety of
clients.


TweetServiceImpl.java
^^^^^^^^^^^^^^^^^^^^^
::

    package tutorial.pubsub;
    
    import javax.inject.Inject;
    
    import io.baratine.core.Journal;
    import io.baratine.core.Lookup;
    import io.baratine.core.OnActive;
    import io.baratine.core.Result;
    import io.baratine.core.ResultStream;
    import io.baratine.core.Service;
    import io.baratine.pubsub.PubSubService;
    
    @Service("public:///tweet")
    public class TweetServiceImpl
    {
      @Inject @Lookup("pubsub:///my-topic")
      private PubSubService<String> _pubsub;
    
      public void subscribe(ResultStream<String> result)
      {
        _pubsub.subscribe(result);
      }
    
      public void publish(String msg)
      {
        _pubsub.publish(msg);
      }
    
      @OnActive
      public void onActive(Result<Void> result)
      {
        System.out.println(getClass().getSimpleName() + " service is now active");
    
        result.complete(null);
      }
    }

This service is using the built-in ``PubSubService`` to manage subscriptions.
All that ``TweetServiceImpl`` is doing in this case is proxying the calls to it.
We need ``TweetServiceImpl`` in the first place because ``pubsub://`` is not
exposed to remote clients like JavaScript.


TweetService.java
^^^^^^^^^^^^^^^^^
::

    package tutorial.pubsub;
    
    import io.baratine.core.ResultStream;
    
    public interface TweetService
    {
      void subscribe(ResultStream<String> result);
    
      void publish(String tweet);
    }


The Java Client
^^^^^^^^^^^^^^^
::

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
    
          // websocket connections may time out after inactivity
          if (e instanceof ServiceExceptionClosed) {
            System.out.println(getClass().getSimpleName() + ".fail: timed out, reconnecting");
    
            _service.subscribe(new MyResultStream(_service));
          }
        }
      }
    }

Run the client on the command-line and you'll get::

    $ java -jar tutorial-08-pubsub-jar-with-dependencies.jar
    MyResultStream.accept: hello world

We can publish to our service with::

    $ curl 'http://127.0.0.1:8085/s/pod/tweet?m=publish&p0=aaa'


Conclusion
----------
You have just built a publish/subscribe service that serves cross-platform
clients.  You may continue below for more advanced cases.


Alternative to PubSubService
----------------------------
Instead of delegating to ``PubSubService``, you can manage the subscriptions
yourself, which are the ``ResultStream`` objects.  You may save the
``ResultStream`` in a list and check to see if the client has cancelled it (todo: add doc)
or disconnected by calling ``ResultStream.isCancelled()``::

    @Service("public:///tweet")
    public class TweetServiceImpl
    {
      private List<ResultStream<String>> _list;
      
      public void subscribe(ResultStream<String> result)
      {
        _list.add(result);
      }
    
      public void publish(String msg)
      {
        Iterator<ResultStream<String>> iter = _list.iterator();
        
        while (iter.hasNext()) {
          ResultStream<String> result = iter.next();
          
          if (result.isCancelled()) {
            iter.remove();
          }
          else {
            result.accept(msg);
          }
        }
      }
    
      @OnActive
      public void onActive(Result<Void> result)
      {
        System.out.println(getClass().getSimpleName() + " service is now active");
    
        result.complete(null);
      }
    }
