name := "scala-ipfs-api"
organization := "io.ipfs"
version := "1.0.0-SNAPSHOT"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.2.2"

libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.2.2"

libraryDependencies += "org.specs2" %% "specs2-core" % "3.6.4" % "test"

lazy val publishDir = settingKey[String]("Publishing directory")
lazy val publishIpfs = taskKey[Unit]("Publish to IPFS")

publishDir := {
  import java.nio.file.Files
  Files.createTempDirectory("sbt-ipfs-").toString
}

publishTo := {
  Some(Resolver.file("ipfs", file(publishDir.value)))
}

publishIpfs := {
  publish.value
  ("ipfs add -r " + publishDir.value) !
}
