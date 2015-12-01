.. _examples-clustering:

Tutorial: Setting up a Cluster
==============================


Prerequisites
-------------
This tutorial will show you how to build a sharded service and deploy it to a
cluster.  It assumes that you already know how to build a service with children.


Introduction
------------
Baratine is built from the ground up to support sharding of services across a
cluster of machines.  To take advantage of sharding, all you need to do is to
deploy a service that supports children.  That's all there to it.  Your service
will scale linearly to hundreds, even thousands, of machines effortlessly.

The magic is in the URLs that Baratine uses throughout.  Your service lives at
a URL, with its children living at relative paths of the parent.  For example:

  ``/map`` - parent
  
  ``/map/aaa`` - child
  
  ``/map/bbb`` - child

Baratine hashes the URL to decide which machine to send the request to.
For example, ``/map`` might go to machine #1, ``/map/aaa`` to machine #0, and
``/map/bbb`` to machine #2.  As a consequence, your service will be
instantiated ``N`` number of times, once on each machine, where ``N`` is the
number of machines in the cluster.  In other words, there will be ``N`` copies
of your service running in in the cluster.


GitHub Repository
-----------------
The code for this tutorial is in `GitHub <https://github.com/baratine/tutorials/tree/master/06-clustering>`_.


Pods - Virtual Cluster
----------------------
Baratine supports partitioning a physical cluster of machines into a subset
called a pod, Baratine's term for a virtual cluster.  A pod is
defined by the machines inside it, the clustering strategy, and the deployment
code.


Starting Up the Cluster
-----------------------
Before defining a pod, you'll first need to start up multiple Baratine
instances::

    $ bin/baratine start --seed 127.0.0.1:8085 --seed 127.0.0.1:8086 --port 8085
    $ bin/baratine start --seed 127.0.0.1:8085 --seed 127.0.0.1:8086 --port 8086
    $ bin/baratine start --seed 127.0.0.1:8085 --port 8087

Here we are starting up 3 machines, where the first two are the seeds.  The
seed machines are the master and owner for cluster configuration data.  New
cluster members are dynamic servers that contact the seed for cluster state and
configuration; they may come and go.  You may define multiple seed servers by
appending more ``--seed`` arguments.

.. note::
    
    Seed servers are not required to be started first, but are recommended.

Alternatively, you may define the seed servers in a ``cluster.cf``
configuration file::
    
    cluster {
        server 127.0.0.1:8085;
        server 127.0.0.1:8086;
    }

Here we have defined two seed servers.  To start them, you no longer need to
specify ``--seed``::
    
    $ bin/baratine start --conf cluster.cf --port 8085
    $ bin/baratine start --conf cluster.cf --port 8086
    $ bin/baratine start --conf cluster.cf --port 8087

As before, we're starting two seed servers and one dynamic server.


Creating the Pod
----------------
Now that you have cluster, you can assign servers to pods.  We do that in a
``mypod.cf`` configuration file::
    
    pod "mypod" cluster {
        server address="127.0.0.1:8085";
        server address="127.0.0.1:8086";
    }

Here we're defining a pod, naming it ``mypod``, with two servers.  The servers
may be seed servers or they may be dynamic servers.   This pod has a clustering
strategy of ``cluster``, which simply shards the service across all the
machines in the cluster.  The available strategies are:

    **solo**: no sharding, only 1 instance of your service running
    
    **pair**: sharded across 2 machines
    
    **triad**: sharded across 3 machines
        
    **cluster**: sharded across all the machines

.. note::

    The default pod is named ``pod`` with `solo` strategy and consists of just
    one machine (the very first one).  You can change ``pod`` similarly by
    creating a ``.cf`` file.

Once we have a ``.cf``, we'll need to copy it into ``bfs:///config/pods`` (BFS
is Baratine's distributed file system built with just ordinary Baratine
services).  The seed server watches that directory for changes and restarts
any changed pods, creating pods if they don't exist before.

::

    $ bin/baratine put mypod.cf bfs:///config/pods


Deploying to the Pod
--------------------
Now we're ready to deploy our service to the pod that we've just created::
    
    $ bin/baratine deploy --pod mypod user.jar

Or the service jar can be defined in the pod's configuration file::

    pod "mypod" cluster {
        server address="127.0.0.1:8085"
        server address="127.0.0.1:8086"
        
        archive /usr/lib/pods/user.jar
    }

Then copy the file into BFS::

    $ bin/baratine put user.jar bfs:///usr/lib/pods/user.jar

Baratine will wait for that file to exist before starting up the pod.  You may
define multiple archive jars.


UserFacadeImpl.java
-------------------
Remote clients are dumb clients that do not know about the cluster topology.
Therefore, we need a facade service to route requests to the right machine
based on the service URL.  Because it is within the Baratine system, the facade
knows about the topology of the cluster and can directly send the request to
the right server.  Another use of the facade service is to act as an
authentication layer or an aggregator of calls to multiple services.

::

    package tutorial.clustering;
    
    import javax.inject.Inject;
    
    import io.baratine.core.Lookup;
    import io.baratine.core.OnLookup;
    import io.baratine.core.Result;
    import io.baratine.core.Service;
    import io.baratine.core.ServiceRef;
    
    @Service("public:///user")
    public class UserFacadeImpl
    {
      @Inject @Lookup("pod://mypod/_user")
      private ServiceRef _ref;
    
      @OnLookup
      public UserFacadeChild onLookup(String url)
      {
        UserService service = _ref.lookup(url).as(UserService.class);
    
        return new UserFacadeChild(service);
      }
    
      static class UserFacadeChild implements UserService
      {
        private UserService _service;
    
        public UserFacadeChild(UserService service)
        {
          _service = service;
        }
    
        public void get(Result<User> result)
        {
          _service.get(result);
        }
    
        public void set(User user, Result<Boolean> result)
        {
          _service.set(user, result);
        }
      }
    }

This facade's job is to proxy requests from public clients to the internal
service.


UserManagerImpl.java
--------------------
The actual service lives at ``@Service("pod://mypod/_user")``.  The ``pod:``
scheme hides the service from remote clients and makes it only available to
internal clients::

    package tutorial.clustering;
    
    import javax.inject.Inject;
    
    import io.baratine.core.Journal;
    import io.baratine.core.Lookup;
    import io.baratine.core.OnActive;
    import io.baratine.core.OnLookup;
    import io.baratine.core.Result;
    import io.baratine.core.Service;
    import io.baratine.core.ServiceRef;
    import io.baratine.store.Store;

    @Journal
    @Service("pod://mypod/_user")
    public class UserManagerImpl
    {
      @Inject @Lookup("store:///user")
      private ServiceRef _storeRef;
    
      @OnLookup
      public UserServiceImpl onLookup(String url)
      {
        // give the child its partition of the Store
        Store<User> store = _storeRef.lookup(url).as(Store.class);
    
        return new UserServiceImpl(store);
      }
    }


UserServiceImpl.java
--------------------
::

    package tutorial.clustering;
    
    import io.baratine.core.Modify;
    import io.baratine.core.OnLoad;
    import io.baratine.core.OnSave;
    import io.baratine.core.Result;
    import io.baratine.store.Store;

    public class UserServiceImpl
    {
      private User _user;
      private Store<User> _store;
    
      public UserServiceImpl(Store<User> store)
      {
        _store = store;
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
        _store.get("user", result.from(value -> onLoadComplete(value)));
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


UserService.java
----------------
::

    package tutorial.clustering;
    
    import io.baratine.core.Result;
    
    public interface UserService
    {
      void get(Result<User> result);
    
      void set(User user, Result<Boolean> result);
    }


Deployment
----------
Build the service jar and you can deploy to a running Baratine instance with::

     $ bin/baratine.sh deploy --pod mypod user.jar


The Client
----------
In the client, note ``mypod`` in the url ``http://127.0.0.1:8085/s/mypod``::

    String id = args[0];

    ServiceClient client = ServiceClient.newClient("http://127.0.0.1:8085/s/mypod").build();
    UserServiceSync service = client.lookup("remote:///user" + id).as(UserServiceSync.class);
        
    System.out.println(service.get());
    
    service.set(new User("first0", "last0"));
    System.out.println(service.get());

Run the client on the command-line and you'll get::

    $ java -cp user-jar-with-dependencies.jar /johnsmith1982
    null
    User[first0 last0]


Conclusion
----------
You have just learned how to start a Baratine cluster and deploy a sharded
service to it.  You should now understand why facade services are needed when
sharding.
