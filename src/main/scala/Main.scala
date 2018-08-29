package edu.nyu.libraries.dlts.aspace

import CLI.CLISupport
import Http.HttpSupport

import java.io._
import java.net.URI
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats
import org.rogach.scallop.exceptions._
import scala.io.Source

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

  //get authenticate
  

  //process the csv file
  process

  def process() { 
    

    //initialize counter
    var i = drop
    
    //itterate through csv file
    Source.fromFile(csv).getLines.drop(drop).take(take).foreach{ line =>
      i = i + 1
      val cols = line.split(",")
      val ao = cols(0)
      val oldTC = JString(cols(1))
      val newTC = JString(cols(2))

      try{

        val json = get(ao).get
        val tc = json.\("instances")(0).\("sub_container").\("top_container").\("ref")
        val title = json.\("title").extract[String]
        println(i + s": $ao: $title")    
        
        (tc == oldTC) match {
          case true => {
            (tc != newTC) match {
              case true => {
              
                val updated = json.mapField {
                    case ("instances", JArray(head :: tail)) => ("instances", JArray(head.mapField {
                      case ("ref", oldTC) => ("ref", newTC)
                      case otherwise => otherwise
                    } :: tail))
                    case otherwise => otherwise
                }

                (updated.\("instances")(0).\("sub_container").\("top_container").\("ref") == newTC) match {
                  case true => {
                    val updatedJson = (compact(render(updated)))
                    postJson(ao, updatedJson)           
                    logger.write(s"$ao $title updated ${oldTC.extract[String]} set to ${newTC.extract[String]} \n")
                    logger.flush
                  }
                  case false => {
                    logger.write(s"could not update $ao, target uri does not match spreadsheet \n")
                    logger.flush  
                  }
               }
            } 
            
            case false => {
              logger.write(s"$ao $title not updated, target uri, ${newTC.extract[String]}, is the same as current uri \n")
              logger.flush
            }
          }
        }
          
        case false => {
          logger.write(s"$ao $title not updated, original uri, ${oldTC.extract[String]}, is not equal to current uri, ${tc.extract[String]} \n" )
          logger.flush
          }
        }
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
