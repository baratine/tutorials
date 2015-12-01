Tutorial: Child Services
========================


Prerequisites
-------------
This tutorial will show you how to build a RESTful service with multiple child
instances living at unique URLs off of the parent.


Introduction
------------
Baratine uses URLs throughout so that services can be easily locatable and
shardable.  In a clustered environment, Baratine hashes the URL to
determine which machine to send the request to.  A request to service `/foo`
will always go to the same machine.  The question becomes: how can you have
multiple instances of a service running?  In other words, how do you shard your
service?  Baratine accomplishes this with child services.


The Child Service
-----------------
A child service is a service that uses the same Inbox queue as the parent
service.  Its URL is the parent's URL suffixed with a path.  For example::

    /user     - parent
    /user/john - child
    /user/dave - child

Child services are separate instances from the parent (for all intents and
purposes, they are separate services).  Whenever a client sends a request to
``/user/john`` for the very first time, Baratine will ask the parent for a
child service by calling the parent's ``@OnLookup`` method::

    @OnLookup
    public UserServiceImpl onLookup(String url)
    {
      return new UserServiceImpl(url);
    }

Subsequent requests to the same URL will use the same child instance.  Baratine
keeps child instances in memory in an LRU, evicting them from memory when
necessary.  A child instance may be on one machine, another child instance on
another.  In effect, instances are distributed among your cluster of machines.

Child services support the same lifecyle annotations as the parent service.
When the child service is first initialized, Baratine will call its ``@OnLoad``.
When the child is going away, Baratine will call its ``@OnSave`` then its
``@OnDestroy``.

.. important::

    Your ``@OnLookup`` must not block and must not call out to other services
    because Baratine expects it to return immediately.  Instead, initialization
    and loading should be done in your child's ``@OnInit`` and ``@OnLoad``
    respectively.


UserManagerImpl.java - the Parent Service
-----------------------------------------
::

    package tutorial.children;

    @Service("/user")
    public class UserManagerImpl
    {
      @OnLookup
      public UserServiceImpl onLookup(String url)
      {
        return new UserServiceImpl(url);
      }
    }

.. note::

    The parent service is just an ordinary Baratine service.  Therefore,
    clients may call methods on the parent using the parent's URL.


UserServiceImpl.java - the Child Service
----------------------------------------
::

    package tutorial.children;

    import io.baratine.core.Modify;
    import io.baratine.core.OnLoad;
    import io.baratine.core.OnSave;
    import io.baratine.core.Result;
    import io.baratine.core.Service;
    import io.baratine.store.Store;

    public class UserServiceImpl implements UserService
    {
      private User _user;
    
      private transient String _url;
      
      @Inject @Lookup("store:///user")
      private transient void Store<Long> _store;
      
      public UserServiceImpl(String url)
      {
        _url = url;
        
        _user = new User();
      }
      
      @OnInit
      public void onInit(Result<Void> result)
      {
        _store.lookup(url, result.from(store -> _store = store));
      }
      
      public void get(Result<User> result)
      {
        result.complete(_user);
      }
      
      @Modify
      public void set(User user, Result<Boolean> result)
      {
        _user = user;
        
        result.complete(true);
      }
      
      @OnLoad
      public void onLoad(Result<Void> result)
      {
        _store.get("user", result.from(user -> onLoadComplete(user)));
      }
      
      private Void onLoadComplete(User user)
      {
        _user = user;
        
        return null;
      }
      
      @OnSave
      public void onSave(Result<Void> result)
      {
        if (_user != null) {
          _store.put("user", _user, result);
        }
        else {
          _store.remove("user", result);
        }
      }
    }

The service child should save data in its local ``Store``.  Its local ``Store``
is the one with the same URL as the child, thus gauranteeing that the data is
persisted within the same JVM.  For example, the child ``/user/john`` should
save its data in ``store:///user/john``.


UserService.java
----------------
::

    package tutorial.persistence;
    
    import io.baratine.core.Result;
    
    public interface UserService
    {
      void get(Result<User> result);
      
      void set(User user, Result<Boolean> result);
    }


UserServiceSync.java
--------------------
::

    package tutorial.persistence;
        
    public interface UserServiceSync extends UserService
    {
      User get();
      
      boolean set(User user);
    }


Deployment
----------
Deployment is the same as before.  Just deploy the parent service::

     $ bin/baratine.sh deploy tutorial-04-children-jar-with-dependencies.jar

Or you can deploy it as an embedded service programmatically::

     ServerBaratine server = Baratine.newServer()
                                     .port(8085)
                                     .root("file:/tmp/caucho/qa")
                                     .build()
     
     ServiceManager manager = server.newPod("pod")
                                    .build()
                                    .manager();
     
     ServiceRef ref = manager.newService()
                             .address("public:///user")
                             .service(new UserManagerImpl())
                             .build();
     
     Thread.currentThread().join();


The Client
----------
::

    String url = args[0];
    ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/pod").build();
    
    UserServiceSync service = client.lookup("remote://" + url).as(UserServiceSync.class);
    
    System.out.println("get() response is: " + service.get());
    System.out.println("set() response is: " + service.set(new User("foo", "bar")));
    System.out.println("get() response is: " + service.get());
    
Run the client on the command-line and you'll get::

    $ java -jar tutorial-04-children-jar-with-dependencies /user/john
    get() response is null
    set() response is true
    get() response is User[foo bar]


Conclusion
----------
You have just learned how to implement a service with multiple child
instances.  The :doc:`next tutorial <tutorial-05-microservices>` will go over
the concept of facade services for remote clients, which will be important once
we deploy to a clustered environment.

