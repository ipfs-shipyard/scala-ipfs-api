package org.ipfs.api

import java.net.{URLEncoder, URL}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class Client(val host : String, val port: Int,
             val base: String = "/api/v0",
             val protocol: String = "http") {

  val jsonMapper = new ObjectMapper()
  jsonMapper.registerModule(DefaultScalaModule)

  def request(stem: String, query: Seq[(String, String)]) = {
    val url = Client.buildUrl(protocol, host, port, base, stem, query)
    scala.io.Source.fromURL(url)
  }

  def request[T](stem: String, clazz: Class[T], query: Seq[(String, String)] = Seq()) : T = {
    jsonMapper.readValue(request(stem, query).reader(), clazz)
  }

  def swarmPeers : SwarmPeers =  request("/swarm/peers", classOf[SwarmPeers])

  def blockStat(key: String) : BlockStat = request("/block/stat", classOf[BlockStat], Seq("arg" -> key))

}

case class SwarmPeers(Strings: List[String])

case class BlockStat(Key: String, Size: Int)


object Client {

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
    println("Here")
    val client = new Client("localhost", 5001)

    println(client.swarmPeers)

    println(client.blockStat("QmaTEQ77PbwCzcdowWTqRJmxvRGZGQTstKpqznug7BZg87"))
  }

}
