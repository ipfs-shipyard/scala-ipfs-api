# scala-ipfs-api

[![](https://img.shields.io/badge/made%20by-Protocol%20Labs-blue.svg?style=flat-square)](http://ipn.io)
[![](https://img.shields.io/badge/project-IPFS-blue.svg?style=flat-square)](http://ipfs.io/)
[![](https://img.shields.io/badge/freenode-%23ipfs-blue.svg?style=flat-square)](http://webchat.freenode.net/?channels=%23ipfs)
[![standard-readme compliant](https://img.shields.io/badge/standard--readme-OK-green.svg?style=flat-square)](https://github.com/RichardLitt/standard-readme)

> A wrapper of the IPFS Client HTTP-API for Scala.

A JVM client library for interacting with IPFS from Java/Scala environments.

### Documentation

A full java-doc site is available [here](http://ipfs.github.io/scala-ipfs-api/#org.ipfs.api.Client).

### Including in your Java/Scala project
##### SBT:
```
resolvers += "scala-ipfs-api" at "https://ipfs.io/ipfs/QmbWUQEuTtFwNNg94nbpVY25b5PAyPQTd7HhkDsGhRG8Ur/"
libraryDependencies += "io.ipfs" %% "scala-ipfs-api" % "1.0.0-SNAPSHOT",
```

##### Manually:
Although this is considered a bad practice, you may still want to manually put fat-jar generated from assembly task into your classpath, take a look at stand-alone jar creation below.

### Java usage

A working Java example available [here](https://github.com/ipfs/scala-ipfs-api/blob/master/src/main/java/org.ipfs.api.example/Example.java).
 
IPFS API calls are available using a Client instance.

`Client client = new Client("localhost", 5001, "/api/v0", "http");`

Almost all of the API calls return strongly typed POJOs, for example the node configuration can be retrieved with 

`ConfigShow configShow = client.configShow();`

#####  Adding and retrieving data

```
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
```

### Building

The package can be built using [sbt](http://www.scala-sbt.org/).

##### compiling

Compile the project with

> sbt compile

##### package sources only

To build a Java archive, use

> sbt packageBin  

which will create a jar like target/scala-2.10/scala-ipfs-api_2.10-1.0.0-SNAPSHOT.jar

##### stand-alone jar
To create a stand-alone jar that includes all dependencies, do
> sbt assembly

which will create a jar 

> target/scala-2.10/ipfs.api.jar 
### Testing

Unit / Integration tests can be run with

> sbt test

## Contribute

Feel free to join in. All welcome. Open an [issue](https://github.com/ipfs/scala-ipfs-api/issues)!

This repository falls under the IPFS [Code of Conduct](https://github.com/ipfs/community/blob/master/code-of-conduct.md).

[![](https://cdn.rawgit.com/jbenet/contribute-ipfs-gif/master/img/contribute.gif)](https://github.com/ipfs/community/blob/master/contributing.md)

## License

[MIT](LICENSE)
