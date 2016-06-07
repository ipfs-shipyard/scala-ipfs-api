package io.ipfs.api

import java.io._
import java.net.{HttpURLConnection, URL, URLEncoder}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import collection.JavaConverters._
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import java.util.Random

import scala.collection.mutable
import Client._

import scala.io.{BufferedSource, Source}

class Client(val host : String,
             val port: Int = 5001,
             val base: String = "/api/v0",
             val protocol: String = "http") {


  ////////////////
  // Basic commands

  /**
   * Adds local file contents to ipfs
   *
   * @param paths Files to be added
   * @return
   */
  def add(paths: Array[Path]): Array[Add] = jsonMapper.reader(classOf[Add])
    .readValues(upload("/add", paths).reader())
    .readAll()
    .toArray[Add](new Array[Add](0))

  /**
    * Recursively add the directory contents to ipfs
    * @param path Root node to be added
    * @return
    */
  def addTree(path: Path) = add(walkTree(path))

  /**
   *  Show ipfs object data
   * @param key The path of the ipfs object to be outputted
   * @return
   */
  def cat(key: String): InputStream = getRequestInputStream("/cat", toArgs(key))

  /**
   * Stream ipfs object data
   * @param key The path of the ipfs object to be outputted
   * @return
   */

  def get(key: String): InputStream = getRequestInputStream("/get", toArgs(key))

  /**
   * Writes ipfs object data to a local file
   * @param key The path of the ipfs object to be outputted
   * @param output Path to store ipfs object data
   */
  def get(key: String, output: Path): Unit = {
    val in: InputStream = getRequestInputStream("/get", toArgs(key))
    Files.copy(in, output, StandardCopyOption.REPLACE_EXISTING)
    in.close()
  }

  /**
   * List links from an ipfs object.
   * @param key The path of the ipfs object to list links from
   * @return
   */
  def ls(key: String): Ls = getRequestAsType("/ls", classOf[Ls], toArgs(key))

  /**
   * Lists links (references) from an ipfs object
   * @param key The path of the ipfs object to list refs from
   * @return
   */
  def refs(key: String): Array[Ref] = getRequestAsSeq("/refs", classOf[Ref], toArgs(key)).toArray

  ////////////////
  // Data structure commands

  /**
   * Get a raw IPFS block
   * @param key The base58 multihash of an existing ipfs block to get
   * @return
   */
  def blockGet(key: String): InputStream = getRequestInputStream("/block/get", toArgs(key))

  /**
   * Print information of a raw IPFS block
   * @param key the base58 multihash of an existing ipfs block to get
   * @return
   */
  def blockStat(key: String): BlockStat = getRequestAsType("/block/stat", classOf[BlockStat], toArgs(key))

  /**
   *  Stores input as an IPFS block
   * @param key
   * @param in An input stream of data to be stored as an IPFS block
   * @return
   */
  def blockPut(key: String, in: InputStream) = upload("/block/put", Array((key, in)))

  /**
   * Outputs the raw bytes in an IPFS object
   * @param key key of the object to retrieve, in base58-encoded multihash format
   * @return
   */
  def objectData(key: String): InputStream = getRequestInputStream("/object/data", toArgs(key))

  /**
   *  Get and serialize the DAG node named by <key>
   * @param key Key of the object to retrieve (in base58-encoded multihash format)
   * @return
   */
  def objectGet(key: String): ObjectGet = getRequestAsType("/object/get", classOf[ObjectGet], toArgs(key))

  /**
   * Get stats for the DAG node named by <key>
   * @param key Key of the object to retrieve (in base58-encoded multihash format)
   * @return
   */
  def objectStat(key: String): ObjectStat = getRequestAsType("/object/stat", classOf[ObjectStat], toArgs(key))

  /**
   * Outputs the links pointed to by the specified object
   * @param key Key of the object to retrieve, in base58-encoded multihash format
   * @return
   */
  def objectLinks(key: String): Object = getRequestAsType("/object/links", classOf[Object], toArgs(key))

  /**
   * Stores input as a DAG object, outputs its key
   * @param path Input file path formatted like an org.ipfs.api.ObjectGet eg.

        {
            "Data": "another",
            "Links": [ {
                "Name": "some link",
                "Hash": "QmXg9Pp2ytZ14xgmQjYEiHjVjMFXzCVVEcRTWJBmLgR39V",
                "Size": 8
            } ]
        }

   * @return org.ipfs.api.Object
   */
  def objectPut(path: Path): Object = {
    val paths: Array[Path] = Array(path)
    jsonMapper.readValue(upload("/object/put", paths).reader(), classOf[Object])
  }

  /**
   * List directory contents for Unix-filesystem objects
   * @param key The path to the IPFS object(s) to list links from
   * @return
   */
  def fileLs(key: String): FileLs = getRequestAsType("/file/ls", classOf[FileLs], Array("arg" -> key))

  /**
   * Perform a garbage collection sweep on the repo
   */
  def gc(): Unit = {
    getRequestSource("/repo/gc", Seq())
  }

  ////////////////
  // Advanced commands

  /**
   * Gets the value currently published at an IPNS name
   * @param key The name to resolve
   * @return
   */
  def resolve(key: String): Resolve = getRequestAsType("/name/resolve", classOf[Resolve], Array("arg" -> key))

  /**
   * Publish an object to IPNS
   * @param key
   * @return
   */
  def publish(key: String): Publish = getRequestAsType("/name/publish", classOf[Publish], Array("arg" -> key))

  /**
   * Resolve a DNS link
   * @param address The domain-name name to resolve.
   * @return
   */
  def dnsResolve(address: String): String = getRequestAsJson("/dns", toArgs(address)).get("Path").asText()

  /**
   * Show ID info of IPFS node
   * @return
   */
  def id: Id = getRequestAsType("/id", classOf[Id])

  /**
   *  List objects pinned to local storage
   * @return
   */
  def lsPins: JsonNode = getRequestAsJson("/pin/ls", Seq()).get("Keys")

  /**
   * Pins objects to local storage
   * @param key Path to object to be pinned
   * @return Path of object that have been pinned objects
   */
  def addPin(key: String): Array[String] = getRequestAsJson("/pin/add", toArgs(key)).get("Pinned")
    .asScala
    .toArray
    .map(_.toString)

  /**
   * Unpin an object from local storage
   * @param key Path to object to be unpinned
   * @return Path of object that have been unpinned objects
   */
  def removePin(key: String): Array[String] = getRequestAsJson("/pin/rm", toArgs(key)).get("Pinned")
    .asScala
    .toArray
    .map(_.toString)

  ////////////////
  // Network  commands

  def bootstrap: Bootstrap = getRequestAsType("/bootstrap", classOf[Bootstrap])

  /**
   * Lists the set of peers this node is connected to
   * @return List peers with open connections
   */
  def swarmPeers: SwarmPeers = getRequestAsType("/swarm/peers", classOf[SwarmPeers])

  /**
   * Lists all addresses this node is aware of
   * @return
   */
  def swarmAdds: SwarmAddrs = getRequestAsType("/swarm/addrs", classOf[SwarmAddrs])

  /**
   * Opens a new direct connection to a peer address
   * @param address The address to connect in ipfs multiaddr format
   * @return
   */
  def swarmConnect(address: String): JsonNode = getRequestAsJson("/swarm/connect", toArgs(address))

  /**
   * Closes a connection to a peer address
   * @param address The address to disconnect from in ipfs multiaddr format
   * @return
   */
  def swarmDisconnect(address: String): JsonNode = getRequestAsJson("/swarm/disconnect", toArgs(address))

  /**
   * Send an echo request packets to a IPFS host
   * @param peerId ID of peer to be pinged
   * @return
   */
  def ping(peerId: String): Array[Ping] = getRequestAsSeq("/ping", classOf[Ping], toArgs(peerId)).toArray

  /**
   * Store the given key value pair in the dht
   * @param key The key to store the value at
   * @param value The value to store
   * @return
   */
  def dhtPut(key: String, value: String): Array[DHTResponse] = getRequestAsSeq("/dht/put", classOf[DHTResponse], Array("arg" -> key, "arg" -> value)).toArray

  /**
   * Retrieve the value stored in the dht at the given key
   * @param key The key to find a value for
   * @return
   */
  def dhtGet(key: String): DHTResponse = getRequestAsType("/dht/get", classOf[DHTResponse], toArgs(key))

  /**
   * Retrieve a list of peers who are able to provide the value requested
   * @param key The key to find providers for
   * @return
   */
  def dhtFindProvs(key: String): JsonNode = getRequestAsJson("/dht/findprovs", toArgs(key))

  /**
   * @param peerId The peer to search for
   * @return
   */
  def dhtFindPeers(peerId: String): JsonNode = getRequestAsJson("/dht/findpeers", toArgs(peerId))

  /**
   * Run a 'findClosestPeers' query through the DHT
   * @param peerId The peerID to run the query against
   * @return
   */
  def dhtQuery(peerId: String): JsonNode = getRequestAsJson("/dht/query", toArgs(peerId))

  ////////////////
  // Tool commands

  /**
   * Outputs the content of the host configuration parameters
   * WARNING: Your private key will be included in the output of this command.
   * @return
   */
  def configShow: ConfigShow = getRequestAsType("/config/show", classOf[ConfigShow])

  /**
   * Shows ipfs version information
   * @return
   */
  def version: String = getRequestAsJson("/version", Seq()).get("Version").asText()

  /**
   * Lists all available commands (and subcommands)
   * @return
   */
  def commands: JsonNode = getRequestAsJson("/commands", Seq())


  private def buildUrl(stem: String, query: Seq[(String, String)]): URL = Client.buildUrl(protocol, host, port, base, stem, query)

  private def  getRequestInputStream(stem: String, query: Seq[(String, String)]) = {
    val url = buildUrl(stem, query)
    url.openConnection().asInstanceOf[HttpURLConnection].getInputStream
  }

  private def getRequestAsType[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()): T = {
    jsonMapper.readValue(getRequestSource(stem, query).reader(), clazz)
  }
  private def getRequestAsSeq[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()): Seq[T] = {
    //necessary for a few IPFS API calls that  return a concatenated array of json docs instead of a
    //valid JSON doc
    jsonMapper.reader(clazz)
      .readValues(getRequestSource(stem, query).reader())
      .readAll()
      .asScala
  }

  private def getRequestSource(stem: String, query: Seq[(String, String)]) = {
    val url = buildUrl(stem, query)
    scala.io.Source.fromURL(url)
  }

  private def getRequestAsJson(stem: String, query: Seq[(String, String)]): JsonNode = jsonMapper.readTree(getRequestSource(stem, query).reader())

  private def upload(stem: String, namedInputStreams: Array[(String, InputStream)]): BufferedSource = {
    val url = buildUrl(stem, Array("stream-channels" -> "true"))

    val conn = url.openConnection().asInstanceOf[HttpURLConnection]
    conn.setDoOutput(true)
    conn.setDoInput(true)
    conn.setUseCaches(false)
    conn.setRequestMethod("POST")
    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
    conn.setRequestProperty("User-Agent", "Scala IPFS Client")

    val out = conn.getOutputStream
    val writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)

    namedInputStreams.foreach(e => {
      val (name, in) = e
      val headers: Array[String] = Array(
        "--" + boundary,
        "Content-Disposition: file; name=\""+name+"\"; filename=\""+ name+"\"",
        "Content-Type: application/octet-stream",
        "Content-Transfer-Encoding: binary")

      headers.foreach(writer.append(_).append(LINE))
      writer.append(LINE)
      writer.flush()
      try {
        val buffer = new Array[Byte](0x1000)
        var nRead = 0
        while({ nRead = in.read(buffer); nRead } != -1)
          out.write(buffer, 0, nRead)

      } finally {
        out.flush()
        writer.append(LINE)
        writer.flush()
        in.close()
      }
    })

    Array("--", boundary, "--", LINE).foreach(writer.append(_))
    writer.close()

    Source.fromInputStream(conn.getInputStream)
  }

  lazy private val boundary = {
    val random = new Random()
    (0 to 32).map(_ => (0x41 + random.nextInt(26)).asInstanceOf[Char]).toArray.mkString
  }
}

case class Add(Name: String, Hash: String)

case class SwarmPeers(Strings: List[String])

case class BlockStat(Key: String, Size: Int)

case class Link(Name: String, Hash: String, Size: Int, Type: String)
case class Object(Hash: String, Size: Option[Int], Type: Option[String], Links: Array[Link])
case class ObjectGet(Links:Array[Link], Data: String)
case class ObjectStat(Hash: String, NumLinks: Int, BlockSize: Int, LinksSize: Int, DataSize: Int, CumulativeSize: Int)

case class Ls(Objects: Array[Object])

case class Id(ID: String, PublicKey: String, Addresses: List[String], AgentVersion: String, ProtocolVersion: String)

case class Bootstrap(Peers: List[String])

case class Addrs() {
  val map = new mutable.HashMap[String, Array[String]]()
  @JsonAnySetter def set(key: String, addrs: Array[String]) {map.put(key, addrs)}
}
case class SwarmAddrs(Addrs: Addrs)

case class Resolve(Path: String)
case class Publish(Name: String, Value: String)
case class Identity(PeerID: String, PrivKey: String)
case class Datastore(Type: String, Path: String)
case class Addresses(Swarm: Array[String], API: String, Gateway:String)
case class Mounts(IPFS: String, IPNS: String, FuseAllowOther: Boolean)
case class Version(Current: String, Check: String, CheckDate: String, CheckPeriod: String, AutoUpdate: String)
case class MDNS(Enabled: Boolean, Interval: Int)
case class Discovery(MDNS: MDNS)
case class Tour(Last: String)
case class Gateway(HTTPHeaders: String, RootRedirect: String, Writable: Boolean)
case class SupernodeRouting(Servers: Array[String])
case class API(HTTPHeaders: String)
case class Swarm(AddrFilters: String)
case class Log(MaxSizeMB: Int, MaxBackups: Int, MaxAgeDays: Int)
case class ConfigShow(Identity: Identity,
                      Datastore: Datastore,
                      Addresses: Addresses,
                      Mounts: Mounts,
                      Version: Version,
                      Discovery: Discovery,
                      Bootstrap: Array[String],
                      Tour: Tour,
                      Gateway: Gateway,
                      SupernodeRouting: SupernodeRouting,
                      API: API,
                      Swarm: Swarm,
                      Log: Log)

case class Ref(Ref: String, Err: String)

case class Ping(Success: String, Time: Long, Text: String)

case class Arguments() {
  val map = new mutable.HashMap[String, String]()
  @JsonAnySetter def set(key: String, value: String) {map.put(key, value)}
}
case class Objects() {
  val map = new mutable.HashMap[String, Object]()
  @JsonAnySetter def set(key: String, o: Object) {map.put(key, o)}
}
case class FileLs(Arguments: Arguments, Objects: Objects)

case class DHTResponseAddrs(Addrs: Array[String], ID: String)
case class DHTResponse(Extra: String, ID: String, Responses: Array[DHTResponseAddrs], Type: Int)

object Client {
  private val jsonMapper = new ObjectMapper()
  jsonMapper.registerModule(DefaultScalaModule)

  private val LINE = "\r\n"


  class FullyReadableInputStream(in: InputStream) {
    def toArray : Array[Byte] = {
      val out = new ByteArrayOutputStream()
      try {
        val buff = new Array[Byte](0x1000)
        var nRead = 0
        while ( {nRead = in.read(buff);nRead} != -1)
          out.write(buff, 0, nRead)
      } finally {
        in.close()
      }
      out.toByteArray
    }
  }

  implicit def inputStreamToFullyReadableInputStream(in: InputStream): FullyReadableInputStream = new FullyReadableInputStream(in)

  private def toArgs(key: String*): Seq[(String, String)] = key.map("arg" -> _)

  private def urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

  implicit def pathToFile(path: Path): File = path.toFile


  implicit def pathsToNamedInputStreams(paths: Array[Path]): Array[(String, InputStream)] = paths.map(path => (path.getFileName.toString,  new FileInputStream(path)))

  def walkTree(path: Path) : Array[Path] = path match {
    case _ if path.isFile => Array(path)
    case _ if path.isDirectory => path.listFiles().flatMap(f => walkTree(f.toPath))
    case _ => Array()
  }

  private def buildUrl(protocol: String,
               host: String,
               port: Int,
               base: String,
               stem: String,
               query: Seq[(String, String)]) = {

    val queryStem = "?" + query.map(e => urlEncode(e._1) + "=" + urlEncode(e._2)).mkString("&")
    val path = base + stem + queryStem
    new URL(protocol, host, port, path)
  }

  def main(args: Array[String]) = {

    val client = new Client("localhost")

    val sep = () => println("*"*50)

    val paths = Array("build.sbt", "README.md").map(Paths.get(_))
    val add = client.add(paths)
    println(add)

    sep()

    val addedHash: String = add.head.Hash

    val published: String = "Qmdc5rHtRJEdvde9wecKGLmEoHZRqxbvv1Bb6jFHSMKvZZ"
    val resolve = client.resolve(published)
    println(resolve)

    val cat: InputStream = client.cat(addedHash)
    println(Source.fromInputStream(cat).mkString)

    sep()

    val get: InputStream = client.get(addedHash)
    println(Source.fromInputStream(get).mkString)
    sep()


    val id = client.id
    println(id)
    sep()

    val  bootstrap =  client.bootstrap
    println(bootstrap)
    sep()

    val swarmAddrs = client.swarmAdds
    println(swarmAddrs.Addrs.map)
    sep()

    val gc = client.gc()
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

    val blockGet = client.blockGet(addedHash).toArray

    println(blockGet.length)
    sep()

    val objectData = client.objectData(addedHash).toArray
    println(objectData.length)
    sep()

    val objectLinks =  client.objectLinks(addedHash)
    println(objectLinks)
    sep()

    val objectGet = client.objectGet(addedHash)
    println(objectGet)
    sep()

    val objectStat = client.objectStat(addedHash)
    println(objectStat)
    sep()

    val head = Paths.get("/","home", "chrirs", "camping.md")
    val blockPut = client.blockPut(head.getFileName.toString, new FileInputStream(head.toFile))
    println(blockPut.mkString)
    sep()

    val objectPutPath: Path = Paths.get("/", "home", "chrirs", "node.json")
    val objectPut  = client.objectPut(objectPutPath)
    println(objectPut)
    sep()

    val fileLs = client.fileLs(addedHash)
    println(fileLs.Objects.map)
    sep()

    val publish = client.publish(addedHash)
    println(publish)
    sep()

    val dhtKey: String = "ckey3"
    val dhtput  = client.dhtPut(dhtKey, "cval3")
    println(dhtput)
    sep()

    val dhtGet = client.dhtGet(dhtKey)
    println(dhtGet)
    sep()

    val dns = client.dnsResolve("ipfs.io")
    println(dns)
    sep()

    val pinAdd = client.addPin(addedHash)
    println(pinAdd)
    sep()

    val pins: JsonNode = client.lsPins
    println(pins)
    sep()

    val pinRm = client.removePin(addedHash)
    println(pinRm)
    sep()
  }

}
