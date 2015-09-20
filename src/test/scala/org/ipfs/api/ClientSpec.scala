package org.ipfs.api

import java.nio.file.{StandardOpenOption, Paths, Files}

import org.specs2.mutable._

import scala.util.Random

class ClientSpec extends Specification {
  isolated

  val client = new Client("localhost")
  val random = new Random(666)

  "IPFS client " should {

    "show the version" in  {
      client.version mustEqual("0.3.8-dev")
    }

    "have an ID" in {
      client.id.ID.length  mustNotEqual(0)
    }

    "store data" in {
      val buffer = new Array[Byte](0x1000)
      random.nextBytes(buffer)
      val name: String = "random.test.dat"
      val storePath = Paths.get(name)
      Files.write(storePath, buffer, StandardOpenOption.CREATE)
      val add: Array[Add] = client.add(Array(storePath))
      add.length mustEqual(1)
      val added = add(0)
      added.Name mustEqual name
      added.Hash.length mustNotEqual(0)
    }
  }


}
