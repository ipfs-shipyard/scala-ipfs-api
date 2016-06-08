package io.ipfs.api

import java.io.{ByteArrayOutputStream, InputStream}
import java.nio.file.{Files, Paths, StandardOpenOption}
import java.util

import io.ipfs.api.ClientSpec._
import org.specs2.mutable._

import scala.util.Random

class ClientSpec extends Specification {
  isolated

  val client = new Client("localhost")
  "IPFS client" should {

    "show the version" in  {
      client.version mustEqual "0.4.2"
    }

    "have an ID" in {
      client.id.ID.length mustNotEqual 0
    }

    "store data" in {
      val name = randomName
      val add = store(name = name)
      add.length mustEqual 1
      val added = add(0)
      added.Name mustEqual name
      added.Hash.length mustNotEqual 0
    }

    "cat data" in {
      val data = randomBytes
      val added = store(data = data)(0)

      val in: InputStream = client.cat(added.Hash)
      util.Arrays.equals(toArray(in), data) mustEqual true
    }

    "dht put and get" in {
      val (key, value) = (random.nextString(10), random.nextString(10))
      val puts: Array[DHTResponse] = client.dhtPut(key, value)
      puts.length mustNotEqual 0

      client.dhtGet(key).Extra mustEqual value
    }
  }

  private def randomBytes = {
    val buffer = new Array[Byte](0x1500)
    random.nextBytes(buffer)
    buffer
  }

  private def store(name: String = randomName, data: Array[Byte] = randomBytes): Array[Add] = {
    val storePath = Paths.get(name)
    Files.write(storePath, data, StandardOpenOption.CREATE)
    client.add(Array(storePath))
  }
}

object ClientSpec {
  val random = new Random(666)
  def randomName: String = random.nextInt()+".test.dat"

  def toArray(in: InputStream): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    try {
      val buff  = new Array[Byte](0x1000)
      var nRead = 0
      while ( {nRead = in.read(buff);nRead} != -1)
        out.write(buff, 0, nRead)
    } finally {
      in.close()
    }
    out.toByteArray
  }
}

