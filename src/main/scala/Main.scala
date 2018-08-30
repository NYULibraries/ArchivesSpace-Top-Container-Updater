package edu.nyu.libraries.dlts.aspace

import java.io._
import org.json4s._
import org.json4s.jackson.JsonMethods._
import scala.io.Source
import Traits._

object Main extends App with CLISupport with HttpSupport{

  println("ACM Top Container Update Tool, v.0.1b")

  //initialize command line arguments
  val cli = getCLI(args)  
  val csv = cli.source()
  val log = cli.log()
  val drop = cli.drop()
  val take = cli.take()
 
  //initialize logs
  val logger = new FileWriter(new File(log))
  val errorLogger = new FileWriter(new File("errors.txt"))

  //process the csv file
  process

  def process() { 
    
    //initialize counter
    var i = drop
    
    //iterate through csv file
    Source.fromFile(csv).getLines.drop(drop).take(take).foreach{ line =>
      i = i + 1
      val cols = line.split(",")
      val ao = cols(0)
      val oldTC = JString(cols(1))
      val newTC = JString(cols(2))

      try {

        val json = get(ao).get
        val tc = json.\("instances")(0).\("sub_container").\("top_container").\("ref")
        val title = json.\("title").extract[String]
        val info = (s"$i\t$ao\t$title")    
        print(info)

        //check that the current top container URI is eqaul to the value from the spreadsheet
        (tc == oldTC) match {
          case true => {

            //check that current top container URI is not already equal to the new value from the spreadsheet
            (tc != newTC) match {
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
                (updated.\("instances")(0).\("sub_container").\("top_container").\("ref") == newTC) match {
                  case true => {
                    val updatedJson = (compact(render(updated)))
                    postJson(ao, updatedJson)
                    println("\tupdate success")           
                    logger.write(s"$info\tsuccess\t\t${oldTC.extract[String]}\t${newTC.extract[String]} \n")
                    logger.flush
                  }
                  //if not log result
                  case false => {
                    val msg = "\tskipped\tjson update failure"
                    println(msg)
                    logger.write(s"$info$msg\n")
                    logger.flush  
                  }
                }
              }

              //if not log the result
              case false => {
                val msg = "\tskipped\talready updated"
                println(msg)
                logger.write(s"$info$msg\n")
                logger.flush
              }
            }
          }

          //if not log the result  
          case false => {
            val msg = "\tskipped\turl mismatch"
            println(msg)
            logger.write(s"$info$msg\n")
            logger.flush
          }
        }  
      //log any unexpected errors  
      } catch {
        case e: Exception => {
          errorLogger.write(e.toString + "\n")
          errorLogger.flush
        }
      }
    }

    //cleanup
    logger.flush
    errorLogger.flush
    logger.close
    errorLogger.close
    client.close
  }

}
