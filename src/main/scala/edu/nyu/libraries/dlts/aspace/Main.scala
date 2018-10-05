package edu.nyu.libraries.dlts.aspace

import java.io.{File, FileWriter}
import java.net.URL
import java.time.Instant
import edu.nyu.libraries.dlts.aspace.CLI.CLISupport
import org.apache.http.impl.client.CloseableHttpClient
import org.json4s.JsonAST.{JArray, JString}
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.io.Source

case class AspaceSession(username: Option[String], password: Option[String], url: Option[URL], source: File, timeout: Int, drop: Option[Int], take: Option[Int], client: Option[CloseableHttpClient], token: Option[String])

object Main extends App with CLISupport {

  println("\nACM Top Container Update Tool, v.1.0\n-------------------------------------\n")

  val session: AspaceSession = setup(args)

  println("\n1. successfully authenticated\n")
  
  process(session, now)
  testUpdates(session, now)
  session.client.get.close

  private def process(session: AspaceSession, now: String): Unit = {
    println("2. running updates")
    
    val writer = new FileWriter(new File(s"topcontainer-update-$now.tsv"))

    getIterator(session).foreach { line =>

      val cols = line.split(",")
      val aoUrl = cols(0)
      val oldTC = JString(cols(1))
      val newTC = JString(cols(2))

      try {

        getAO(session, aoUrl) match {
          //retrieve ao from aspace
          case Some(json) => {
            val tc = json.\("instances")(0).\("sub_container").\("top_container").\("ref")
            val title = json.\("title").extract[String]
            val info = s"$aoUrl\t$title"

            //check that current top container URI is not already equal to the new value from the spreadsheet
            tc != newTC match {
              case true => {
                //check that the current top container URI is eqaul to the value from the spreadsheet
                tc == oldTC match {
                  case true => {
                    //transform the json, replace the top container ref with new value
                    val updated = json.mapField {
                      case ("instances", JArray(head :: tail)) => ("instances", JArray(head.mapField {
                        case ("ref", oldTC) => ("ref", newTC)
                        case otherwise => otherwise
                      } :: tail))
                      case otherwise => otherwise
                    }

                    //check that the updated json has the new target uri
                    updated.\("instances")(0).\("sub_container").\("top_container").\("ref") == newTC match {

                      //post the updated object
                      case true => {
                        println(s"updating $title")
                        writer.write(s"$aoUrl\t$title\tupdated\n")
                        writer.flush()
                        postAO(session, aoUrl, compact(render(updated)))

                      }

                      case false => {
                        println(s"* $title json object update failed")
                        writer.write(s"$aoUrl\t$title\tjson object update failed\n")
                        writer.flush()
                      }
                    }
                  }

                  case false => {
                    println("* $aolurl url mismatch")
                    writer.write(s"$aoUrl\t$title\turl of top container on aspace does not match spreadsheet\n")
                  }
                }
              }

              case false => {
                println(s"$title already updated")
                writer.write(s"$aoUrl\t$title\talready at new url\n")
                writer.flush()
              }
            }
          }

          case None => {
            println(s"$aoUrl does not exist on server")
            writer.write(s"$aoUrl does not exist on server\n")
            writer.flush()
          }
        }

      } catch {
        case e: Exception => {
          println("** " + e.toString + "\n")
        }
      }
    }
    writer.flush()
    writer.close()
  }

  private def testUpdates(aspaceSession: AspaceSession, now: String): Unit = {
    println("\n3. testing urls are updated")
    val now = Instant.now().toString
    val builder = new StringBuilder

    getIterator(session).foreach { line =>

      val cols = line.split(",")
      val aoUrl = cols(0)
      val newTC = JString(cols(2))
      val json = getAO(session, aoUrl).get
      val tc = json.\("instances")(0).\("sub_container").\("top_container").\("ref")
      val title = json.\("title").extract[String]
      newTC == tc match {
        case true =>
        case false =>   {
          println(s"* $title update failed")
          builder.append(s"$aoUrl\t$title\tdid not update\n")
        }
      }
    }

    builder.isEmpty match {
      case true =>
      case false => {
        val writer = new FileWriter(new File(s"topcontainer-errors-$now.tsv"))
        writer.write(builder.mkString)
        writer.flush()
        writer.close()
      }
    }

  }

  private def getIterator(session: AspaceSession): Iterator[String] = {
    take(drop(Source.fromFile(session.source).getLines, session.drop), session.take)
  }

  private def drop(iterator: Iterator[String], dropOption: Option[Int]): Iterator[String] = {
    dropOption match {
      case Some(n) => iterator.drop(n)
      case None => iterator
    }
  }

  private def take(iterator: Iterator[String], takeOption: Option[Int]): Iterator[String] = {
    takeOption match {
      case Some(n) => iterator.take(n)
      case None => iterator
    }
  }

}
