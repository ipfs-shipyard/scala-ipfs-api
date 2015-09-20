# scala-ipfs-api

A wrapper of the IPFS Client HTTP-API for Java/Scala.

### Building

The package  can be  build using [sbt](http://www.scala-sbt.org/).

##### package sources only

To build only the sources in the project,  use

> sbt packageBin  

which will create a jar somewhere like target/scala-2.10/scala-ipfs-api_2.10-1.0.0-SNAPSHOT.jar

##### stand-alone jar 

To create a  stand-alone jar that includes all dependencies  needed for including in a Java/Scala project do 

> sbt assembly

which will create a jar 

> target/scala-2.10/ipfs.api.jar 

### Documentation

[Full java-doc](http://ipfs.github.io/scala-ipfs-api/#org.ipfs.api.Client).

### Including in your Java/Scala project

### Java usage
