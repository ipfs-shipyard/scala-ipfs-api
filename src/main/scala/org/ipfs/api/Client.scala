package org.ipfs.api

import java.io._
import java.net.{HttpURLConnection, URLEncoder, URL}
import java.nio.file.{Paths, Path}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.util.Random

class Client(val host : String, val port: Int,
             val base: String = "/api/v0",
             val protocol: String = "http") {

  def get(key: String) : InputStream = getRequestInputStream("/get", Seq("arg" -> key))

  def add(paths: Seq[Path]) = upload("/add", paths)

  def ls(key:  String): Ls =  getRequestSource("/ls", classOf[Ls], Seq("arg" -> key))




  def add(path: Path) {add(Seq(path))}

  def swarmPeers: SwarmPeers = getRequestSource("/swarm/peers", classOf[SwarmPeers])

  def blockStat(key: String): BlockStat = getRequestSource("/block/stat", classOf[BlockStat], Seq("arg" -> key))



  private val jsonMapper = new ObjectMapper()
  jsonMapper.registerModule(DefaultScalaModule)

  private def  getRequestInputStream(stem: String, query: Seq[(String, String)]) = {
    val url = Client.buildUrl(protocol, host, port, base, stem, query)
    url.openConnection().asInstanceOf[HttpURLConnection].getInputStream
  }

  private def getRequestSource[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()): T = {
    jsonMapper.readValue(getRequestSource(stem, query).reader(), clazz)
  }

  private def getRequestSource(stem: String, query: Seq[(String, String)]) = {
    val url = Client.buildUrl(protocol, host, port, base, stem, query)
    scala.io.Source.fromURL(url)
  }

  private def upload(stem: String, paths: Seq[Path]) {
    val url = Client.buildUrl(protocol, host, port, base, stem, Seq("stream-channels" -> "true"))

    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setDoOutput(true)
    conn.setDoInput(true)
    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)

    val out = conn.getOutputStream
        val writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"))

    val add = (path: Path) => {
      val fileName = path.getFileName.toString

      val headers: Seq[String] = Seq(
        "--" + boundary,
        "Content-Disposition: file; name=\"file\"; filename=\"" + fileName + "\"",
        "Content-Type: application/octet-stream",
        "Content-Transfer-Encoding: binary")

      headers.foreach(writer.append(_).append(Client.LINE))
      writer.flush()

      val in = new FileInputStream(path.toFile)
      try {
        val buffer = new Array[Byte](0x1000)
        var nRead = 0
        while ( {
          nRead = in.read(buffer); nRead
        } != -1)
          out.write(buffer, 0, nRead)
      } finally {
        writer.append(Client.LINE)
        out.flush
        in.close
      }
    }

    paths.foreach(add)

    Seq("--", boundary, "--", Client.LINE).foreach(writer.append(_))
    writer.close
  }

  lazy private val boundary = {
    val random = new Random()
    (0 to 32).map(_ => (0x41 + random.nextInt(26)).asInstanceOf[Char]).toArray.mkString
  }
}

case class SwarmPeers(Strings: List[String])

case class BlockStat(Key: String, Size: Int)

case class Link(Name: String,  Hash: String, Size: Int, Type: Int)
case class Object(Hash: String, Links: Seq[Link])
case class Ls(Objects: Seq[Object])

object Client {

  val LINE = "\r\n"

  def buildUrl(protocol: String,
               host: String,
               port: Int,
               base: String,
               stem: String,
               query : Seq[(String, String)]) = {

    val queryStem = query.map(e => URLEncoder.encode(e._1, "UTF-8")  +"="+ URLEncoder.encode(e._2, "UTF-8"))
      .foldLeft(new StringBuilder("?"))((builder, entry) => builder.append("&").append(entry))
      .toString

    val path = base + stem + queryStem
    new URL(protocol, host, port, path)
  }

  def main(args: Array[String]) = {

    val client = new Client("localhost", 5001)

    println(client.swarmPeers)

    val addedHash = "QmaTEQ77PbwCzcdowWTqRJmxvRGZGQTstKpqznug7BZg87"

    println(client.blockStat(addedHash))

    println(client.ls(addedHash))

    println(io.Source.fromInputStream(client.get(addedHash)).mkString)

    val path = Paths.get("src", "main", "resources", "test.txt")
    client.add(path)

    println(client.getRequestSource("/file/ls", Seq("arg" -> addedHash)).mkString)

  }

}
