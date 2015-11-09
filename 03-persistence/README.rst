Tutorial: Persistence
=====================


Prerequisites
-------------
This tutorial will show you how to save data within your service.  It assumes
that you are already familiar with Baratine's annotation-based lifecycle hooks.


Introduction
------------
Baratine's data model is an in-memory model where data is loaded only once and
stays in memory.  Periodically, data is checkpointed and saved in batches.  This
load-once and save-batch behavior is possible because Baratine encourages
your service to be the owner and authorative copy of the data it's serving.  The
service is the only writer, thereby removing contention caused by having
multiple writers.  Because it is the only writer, the service can batch
saves/writes for efficiency.

.. note::
    
    Because of this batching capability, Baratine is very much like Hadoop,
    Spark, and Storm.  The difference and advantage is that Baratine is
    a real-time service platform that also happens to natively support
    batching.


GitHub Repository
-----------------
The code for this tutorial is in `GitHub <https://github.com/baratine/tutorials/tree/master/03-persistence>`_.


@OnLoad, @OnSave, @Modify
-------------------------
Baratine will call your ``@OnLoad`` method once on startup, and will
periodically call your ``@OnSave`` method to give you an opportunity to save
your data.  How often ``@OnSave`` is called is dependent on load.  It may be
once per method call that is annotated with ``@Modify``.  Or it may be once per
large batch on a heavily-loaded system.

You may save your data however you like.  To make things simple, we'll use the
built-in key-value store: ``io.baratine.Store``.

.. note::
    ``@OnLoad/@OnSave`` are similar to ``@BeforeBatch/@AfterBatch`` but
    different in that they work in tandem with journaling through checkpoints.
    We'll cover journaling later in this tutorial.


CounterServiceImpl.java
-----------------------
::

    package tutorial.persistence;
    
    import javax.inject.Inject;
    
    import io.baratine.core.Lookup;
    import io.baratine.core.Modify;
    import io.baratine.core.OnLoad;
    import io.baratine.core.OnSave;
    import io.baratine.core.Result;
    import io.baratine.core.Service;
    import io.baratine.store.Store;
    
    @Service("public:///counter")
    public class CounterServiceImpl implements CounterService
    {
      private long _count;
    
      @Inject @Lookup("store:///counter")
      private Store<Long> _store;
      
      public void get(Result<Long> result)
      {
        result.complete(_count);
      }
      
      @Modify
      public void addAndGet(long value, Result<Long> result)
      {
        _count += value;
    
        result.complete(_count);
      }
    
      @OnLoad
      public void onLoad(Result<Void> result)
      {
        _store.get("count", result.from(value -> onLoadComplete(value)));
      }
    
      private Void onLoadComplete(Long value)
      {
        _count = value != null ? value : 0;
    
        return null;
      }
    
      @OnSave
      public void onSave(Result<Void> result)
      {
        _store.put("count", _count, result);
      }
    }

Here we are annotating ``addAndGet()`` with ``@Modify`` because it is
modifying the service's internal state.  That annotation signifies to Baratine
that it should call your ``@OnSave`` sometime in the future.

We are injecting in the ``Store`` using ``@Inject`` and ``@Lookup``.  The URL
in ``@Lookup`` should be your service URL, to facilitate locality if you choose
to shard your service in the future (covered in the :doc:`sharding tutorial <tutorial-xxx-sharding>`).


CounterService.java
-------------------
::

    package tutorial.persistence;
    
    import io.baratine.core.Result;
    
    public interface CounterService
    {
      void get(Result<Long> result);
      
      void addAndGet(long value, Result<Long> result);
    }


CounterServiceSync.java
-----------------------
::

    package tutorial.persistence;
    
    import io.baratine.core.Result;
    
    public interface CounterServiceSync extends CounterService
    {
      long get();
      
      long addAndGet(long value);
    }


Deployment
----------
Build the service jar and you can deploy to a running Baratine instance with::

     $ bin/baratine.sh deploy tutorial-03-persistence-jar-with-dependencies.jar

Or you can deploy it as an embedded service programmatically::

     ServerBaratine server = Baratine.newServer()
                                     .port(8085)
                                     .root("file:/tmp/caucho/qa")
                                     .build();
     
     ServiceManager manager = server.newPod("pod")
                                    .build()
                                    .manager();
     
     ServiceRef ref = manager.newService()
                             .address("public:///counter")
                             .service(new CounterServiceImpl())
                             .build();
     
     Thread.currentThread().join();

on the command-line with::

    java -cp tutorial-03-persistence-jar-with-dependencies.jar tutorial.persistence.ServiceMain


The Client
----------
::

    long value = Long.parseLong(args[0]);

    ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build();
    
    CounterService service = client.lookup("remote:///counter").as(CounterService.class);
    
    service.addAndGet(value, value -> System.out.println(value));

Run the client on the command-line and you'll get::

    $ java -jar tutorial-03-persistence-jar-with-dependencies.jar 111
    Received response: 111
    
    $ java -jar tutorial-03-persistence-jar-with-dependencies.jar 444
    Received response: 555


Manually Requesting a Save
--------------------------
You may choose to call your ``@OnSave`` manually, instead of waiting for Baratine
to call it, by calling your service's ``ServiceRef.save(Result<Boolean>)``::

    ServiceRef.current().save(result);

The save request will be queued into the Inbox.  In other words, the save is
just a normal request and it has to go through the Inbox first.


Journaling with @Journal
------------------------
If a crash were to happen, your ``@OnSave`` may not be called and you may lose
data.  To protect this, Baratine provides a journal on the Inbox with the class
annotation ``@Journal``.  Requests are journaled before they are sent to your
service.  On startup, Baratine will replay requests from the journal before
calling your service's ``@OnActive`` to process normal requests.

::

    import io.baratine.core.Journal;
    import io.baratine.core.OnActive;
    import io.baratine.core.Result;
    import io.baratine.core.Service;

    @Journal
    @Service("public:///counter")
    public class CounterServiceImpl
    {
      ...
      
      @OnActive
      public void onActive(Result<Void> result)
      {
        System.out.println("counter service is now active");
        
        result.complete(null);
      }
    }

When Baratine initiates a checkpoint (i.e. rolling over the journal), it will
call your ``@OnSave`` to save your data before flushing the journal.  The
checkpoint along with the saved data represents a clean snapshot in time of your
service's state.


Journal Side-effects
--------------------
With a journal, your service may see two copies of the same request if a crash
were to happen:

  #. once during normal operation, then server crashed before ``@OnSave``
  #. again during the journal replay on startup

Your service may want to detect journal replays and decide, for example, to not
send out emails for replayed requests.  If your ``@OnActive`` hasn't been
called yet, then you would know that your service is in a replay state.


Conclusion
----------
You have just learned how to use ``@OnLoad``, ``@OnSave``, and ``@Modify`` to
save your data in batches.  Together with ``@Journal`` and ``@OnActive``, you
can start building reliable **persistent** services that can scale to millions of
requests per second.  The :ref:`next tutorial <tutorial-04-child-services>`
will go over child services used to implement sharding.
