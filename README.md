# scala-ipfs-api

A JVM client library for interacting with IPFS from Java/Scala environments.

### Documentation

A full java-doc site is available [here](http://ipfs.github.io/scala-ipfs-api/#org.ipfs.api.Client).

### Including in your Java/Scala project

Copy ipfs.api.jar into your Java classpath.

### Java usage

A working Java example available [here](https://github.com/ipfs/scala-ipfs-api/blob/master/src/main/java/org.ipfs.api.example/Example.java).
 
IPFS API calls are available using a Client instance.

<pre>
Client client = new Client("localhost", 5001, "/api/v0", "http");
</pre>

Almost all of the API calls return strongly typed POJOs, for example the node configuration can be retrieved with 

<pre>
ConfigShow configShow = client.configShow(); 
</pre>

#####  Adding and retrieving data

<pre>
        //create test file
        Path addPath = Paths.get("ipfs.put.tmp.txt");
        Files.write(addPath, "Hello IPFS!".getBytes(), StandardOpenOption.CREATE);

        //add to IPFS
        Add[] add = client.add(new Path[]{addPath});
        Add added = add[0];
        System.out.println("Added "+ added.Name() +" with hash "+  added.Hash());

        //get from IPFS
        Path getPath = Paths.get("ipfs.get.tmp.txt");
        try (InputStream inputStream = client.cat(added.Hash())) {
            Files.copy(inputStream, getPath, StandardCopyOption.REPLACE_EXISTING);
        }
</pre>

### Building

The package can be built using [sbt](http://www.scala-sbt.org/).

##### compiling

Compile the project with

> sbt compile

##### package sources only

To build a Java archive of **only the project sources**,  use

> sbt packageBin  

which will create a jar somewhere like target/scala-2.10/scala-ipfs-api_2.10-1.0.0-SNAPSHOT.jar

##### stand-alone jar 

To create a stand-alone jar that includes all dependencies needed for including in a Java/Scala project do 

> sbt assembly

which will create a jar 

> target/scala-2.10/ipfs.api.jar 

Note, this includes the (only) project dependency [Jackson](https://github.com/FasterXML/jackson)

### Testing

Unit / Integration tests can be run  with

> sbt test