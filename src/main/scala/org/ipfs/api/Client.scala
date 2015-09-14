package org.ipfs.api

import java.io._
import java.net.{HttpURLConnection, URLEncoder, URL}
import java.nio.file.{Paths, Path}
import collection.JavaConverters._

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.util.Random

import scala.collection.mutable

import Client._

class Client(val host : String,
             val port: Int = 5001,
             val base: String = "/api/v0",
             val protocol: String = "http") {

  def cat(key: String) : InputStream = getRequestInputStream("/cat", toArgs(key))

  def add(paths: Seq[Path]) = upload("/add", paths)

  def ls(key:  String): Ls =  getRequestAsJson("/ls", classOf[Ls], toArgs(key))

  def refs(key: String): Seq[Ref] = getRequestJsonSeq[Ref]("/refs", classOf[Ref], toArgs(key))



  //TODO  blockPut
  def blockGet(key: String) : InputStream = getRequestInputStream("/block/get",  toArgs(key))

  def objectData(key : String) : InputStream = getRequestInputStream("/object/data", toArgs(key))

  def objectGet(key: String) : ObjectGet = getRequestAsJson[ObjectGet]("/object/get", classOf[ObjectGet], toArgs(key))

  def objectLinks(key: String): Object  = getRequestAsJson[Object]("/object/links", classOf[Object], toArgs(key))

  def ping(key: String) : Seq[Ping] = getRequestJsonSeq[Ping]("/ping", classOf[Ping], toArgs(key))

  def add(path: Path) {add(Seq(path))}

  def swarmPeers: SwarmPeers = getRequestAsJson("/swarm/peers", classOf[SwarmPeers])

  def blockStat(key: String): BlockStat = getRequestAsJson("/block/stat", classOf[BlockStat], toArgs(key))

  def id : Id = getRequestAsJson("/id", classOf[Id])

  def bootstrap : Bootstrap = getRequestAsJson("/bootstrap", classOf[Bootstrap])

  def swarmAdds: SwarmAddrs = getRequestAsJson("/swarm/addrs", classOf[SwarmAddrs])

  def gc {getRequestSource("/repo/gc", Seq())}

  def configShow : ConfigShow =  getRequestAsJson("/config/show", classOf[ConfigShow])

  def version : APIVersion =  getRequestAsJson("/version", classOf[APIVersion])

  private val jsonMapper = new ObjectMapper()
  jsonMapper.registerModule(DefaultScalaModule)

  private def  getRequestInputStream(stem: String, query: Seq[(String, String)]) = {
    val url = buildUrl(protocol, host, port, base, stem, query)
    url.openConnection().asInstanceOf[HttpURLConnection].getInputStream
  }

  private def getRequestAsJson[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()): T = {
    jsonMapper.readValue(getRequestSource(stem, query).reader(), clazz)
  }
  private def getRequestJsonSeq[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()) : Seq[T] = {
    //necessary for a few IPFS API calls that  return a concatenated sequence of json docs instead of a
    //valid JSON doc
    jsonMapper.reader(clazz)
      .readValues(getRequestSource(stem, query).reader())
      .readAll()
      .asScala

  }


  private def getRequestSource(stem: String, query: Seq[(String, String)]) = {
    val url = buildUrl(protocol, host, port, base, stem, query)
    scala.io.Source.fromURL(url)
  }

  private def upload(stem: String, paths: Seq[Path]) {
    val url = buildUrl(protocol, host, port, base, stem, Seq("stream-channels" -> "true"))

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

      headers.foreach(writer.append(_).append(LINE))
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
        writer.append(LINE)
        writer.flush
        in.close
      }
    }

    paths.foreach(add)

    Seq("--", boundary, "--", LINE).foreach(writer.append(_))
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
case class ObjectGet(Links:Seq[Link], Data: String)

case class Ls(Objects: Seq[Object])

case class Id(ID: String,  PublicKey: String,  Addresses: List[String], AgentVersion: String, ProtocolVersion: String)

case class Bootstrap(Peers: List[String])

case class Addrs() {
  val map = new mutable.HashMap[String, Seq[String]]()
  @JsonAnySetter def set(key: String, addrs: Seq[String]) {map.put(key, addrs)}
}
case class SwarmAddrs(Addrs: Addrs)


case class Identity(PeerID: String,  PrivKey: String)
case class Datastore(Type: String,  Path: String)
case class Addresses(Swarm:  Seq[String], API: String,  Gateway:String)
case class Mounts(IPFS: String,  IPNS: String,  FuseAllowOther: Boolean)
case class Version(Current: String, Check: String, CheckDate: String,  CheckPeriod: String, AutoUpdate: String)
case class MDNS(Enabled: Boolean, Interval: Int)
case class Discovery(MDNS: MDNS)
case class Tour(Last:String)
case class Gateway(HTTPHeaders: String, RootRedirect: String, Writable: Boolean)
case class SupernodeRouting(Servers: Seq[String])
case class API(HTTPHeaders: String)
case class Swarm(AddrFilters: String)
case class Log(MaxSizeMB: Int, MaxBackups: Int, MaxAgeDays: Int)
case class ConfigShow(Identity: Identity,
                      Datastore: Datastore,
                      Addresses: Addresses,
                      Mounts: Mounts,
                      Version: Version,
                      Discovery: Discovery,
                      Bootstrap: Seq[String],
                      Tour: Tour,
                      Gateway: Gateway,
                      SupernodeRouting: SupernodeRouting,
                      API: API,
                      Swarm: Swarm,
                      Log: Log)

case class APIVersion(Version: String)

case class Ref(Ref: String, Err: String)

case class Ping(Success: String, Time: Int, Text:  String)


object Client {

  class FullyReadableInputStream(in: InputStream) {
      def toArray : Array[Byte] = {
      val out = new ByteArrayOutputStream()
      try {
        val buff  = new Array[Byte](0x1000)
        var nRead = 0
        while ( {nRead = in.read(buff);nRead} != -1)
          out.write(buff, 0, nRead)
      } finally {
        in.close
      }
      out.toByteArray
    }
  }

  implicit def inputStreamToFullyReadableInputStream(in: InputStream) = new FullyReadableInputStream(in)

  val LINE = "\r\n"

  def toArgs(key: String) = Seq("arg" -> key)

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

    val client = new Client("localhost")
    //
    //    println(client.swarmPeers)
    //

        val addedHash = "QmaTEQ77PbwCzcdowWTqRJmxvRGZGQTstKpqznug7BZg87"

    //
    //    println(client.blockStat(addedHash))
    //
    //    println(client.ls(addedHash))
    //
    //
    //    val path = Paths.get("src", "main", "resources", "test.txt")
    //    client.add(path)

    //    println(client.getRequestSource("/file/ls", Seq("arg" -> addedHash)).mkString)

    val sep = () => println("*"*50)

    val cat: InputStream = client.cat(addedHash)
    println(io.Source.fromInputStream(cat).mkString)

    sep()


    val pinls =  client.getRequestSource("/pin/ls", Seq()).mkString
    println(pinls)
    sep()

    val  id =  client.id
    println(id)
    sep()

    val  bootstrap =  client.bootstrap
    println(bootstrap)
    sep()

    val swarmAddrs = client.swarmAdds
    println(swarmAddrs.Addrs.map)
    sep()

    val gc = client.gc
    println(gc)
    sep()

    val configShow = client.configShow
    println(configShow)
    sep()

    val version = client.version
    println(version)
    sep()

    val ls = client.ls(addedHash)
    println(ls)
    sep()

    val swarmPeers = client.swarmPeers
    println(swarmPeers)
    sep()

//    val refs = client.refs(addedHash)
//    println(refs)
//    sep()

//    val ping = client.ping(addedHash)
//    println(ping)
//    sep()




    val blockGet =  client.blockGet(addedHash).toArray

    println(blockGet.length)
    sep()

    val  objectData = client.objectData(addedHash).toArray
    println(objectData.length)
    sep()

    val objectLinks =  client.objectLinks(addedHash)
    println(objectLinks)
    sep()

    val objectGet = client.objectGet(addedHash)
    println(objectGet)
    sep()




  }

}
