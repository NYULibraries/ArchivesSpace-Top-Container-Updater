package edu.nyu.libraries.dlts

import com.typesafe.config._

import java.io._
import java.net.URI

import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.{ ClientProtocolException, ResponseHandler }
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{ HttpPost, HttpGet }
import org.apache.http.impl.client.{ HttpClientBuilder, CloseableHttpClient } 
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.apache.http.entity.StringEntity

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats

import scala.io.Source



object Main extends App {

  //initialize configuration
  val conf = ConfigFactory.load()

  //initialize values
  implicit val formats = DefaultFormats
  val csv = args(0)
  val log = args(1)
  val drop = args(2).toInt
  val take = args(3).toInt
  val username = conf.getString("tcUpdater.username")
  val password = conf.getString("tcUpdater.password")
  val aspace = conf.getString("tcUpdater.aspaceApi")
  
  //initialize logs
  val logger = new FileWriter(new File(log))
  val errorLogger = new FileWriter(new File("errors.txt"))
  
  //initialize httpclient
  val timeout = 2;
  val config = RequestConfig.custom()
    .setConnectTimeout(timeout * 1000)
    .setConnectionRequestTimeout(timeout * 1000)
    .setSocketTimeout(timeout * 1000).build();
  val client = HttpClientBuilder.create().setDefaultRequestConfig(config).build()
  val header = "X-ArchivesSpace-Session"
  
  //get authenticate
  val token = getToken
  
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

      val json = get(token, aspace + ao)
      val tc = json.\("instances")(0).\("sub_container").\("top_container").\("ref")
      val title = json.\("title")
      println(i + s": $ao $title")    
      
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
                  post(token, aspace + ao, updatedJson)           
                  logger.write(s"$ao $title updated $oldTC set to $newTC \n")
                  logger.flush
                }
                case false => {
                  logger.write(s"could not update $ao, target uri does not match spreadsheet \n")
                  logger.flush  
                }
             }
          } 
          
          case false => {
            logger.write(s"$ao $title not updated, target uri, $newTC, is the same as current uri, $tc \n")
            logger.flush
          }
        }
      }
        
      case false => {
        logger.write(s"$ao $title not updated, original uri, $oldTC, is not equal to current uri, $tc \n" )
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

  //methods
  def getToken(): String = {
    val authenticate = new HttpPost(aspace + "/users/" + username + "/login?password=" + password)
    val response = client.execute(authenticate)
    val entity = response.getEntity
    val content = entity.getContent
    val data = scala.io.Source.fromInputStream(content).mkString
    val jarray = parse(data)
    val askey = (jarray \ "session").extract[String]
    EntityUtils.consume(entity)
    response.close
    askey
  }

  def get(token: String, url: String): JValue = {
    val get = new HttpGet(url)
    get.addHeader(header, token)
    val response = client.execute(get)
    val entity = response.getEntity
    val content = entity.getContent
    val data = scala.io.Source.fromInputStream(content).mkString
    EntityUtils.consume(entity)
    response.close
    parse(data)
  } 

  def post(token: String, url: String, data: String) {
    val httpPost = new HttpPost(url)
    httpPost.addHeader(header, token)
    val entity = new StringEntity(data, "UTF-8")
    httpPost.setEntity(entity)
    httpPost.setHeader("Content-type", "application/json; charset=UTF-8")
    val response = client.execute(httpPost)
    val code = response.getStatusLine().getStatusCode()
    EntityUtils.consume(entity)
    response.close
  }
  
}
