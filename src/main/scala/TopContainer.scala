package edu.nyu.libraries.dlts.aspace

import com.typesafe.config._
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
import org.rogach.scallop._
import org.rogach.scallop.exceptions._


object Traits {

  //initialize configuration
  val conf = ConfigFactory.load()
  val username = conf.getString("tcUpdater.username")
  val password = conf.getString("tcUpdater.password")
  val aspace = conf.getString("tcUpdater.aspaceApi")
  
  trait HttpSupport {

    implicit val formats = DefaultFormats  
    
    val header = "X-ArchivesSpace-Session"
    
    val client = getClient

    private def getClient(): CloseableHttpClient = {
      val timeout = 2;
      val config = RequestConfig.custom()
        .setConnectTimeout(timeout * 1000)
        .setConnectionRequestTimeout(timeout * 1000)
        .setSocketTimeout(timeout * 1000).build();
      
      HttpClientBuilder.create().setDefaultRequestConfig(config).build()
  
    } 

    val token = getToken(username, password)

    private def getToken(username: String, password: String): String = {
      try {
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
      } catch {
          case e: Exception => {
            throw new Exception("Cannot Authenticate")
            System.exit(1)
            ""
          }
      }
    }

    def get(url: String): Option[JValue] = {
      try {
        val httpGet = new HttpGet(aspace + url)
        httpGet.addHeader(header, token)
        val response = client.execute(httpGet)
        val entity = response.getEntity
        val content = entity.getContent
        val data = scala.io.Source.fromInputStream(content).mkString
        EntityUtils.consume(entity)
        response.close
        Some(parse(data))
      } catch {
        case e: Exception => None
      }
    } 

    def postJson(url: String, data: String): Option[Int] = {
      try {
        val httpPost = new HttpPost(aspace + url)
        httpPost.addHeader(header, token)
        val entity = new StringEntity(data, "UTF-8")
        httpPost.setEntity(entity)
        httpPost.setHeader("Content-type", "application/json; charset=UTF-8")
        val response = client.execute(httpPost)
        val code = response.getStatusLine().getStatusCode().toInt
        EntityUtils.consume(entity)
        response.close
        Some(code)
      } catch {
        case e: Exception => None
      }
    }
  } 

  trait CLISupport { 
    
    def help(optionName: String) {
      println(s"Error: Missing required option $optionName")
      help()
    }

    def error(message: String) {
      println(message)
      println(help)
    }

    def help() {
      println("usage: java -jar TCUpdate.jar [options]")
      println("  options:")
      println("    -s, --source, required\tpath to csv file to be input")
      println("    -l, --log, required\t\tpath to log file to be written")
      println("    -d, --drop, required\tnumber of rows to skip from the beginning of csv file")
      println("    -t, --take, required\tnumber of rows to process from csv file")
      println("    -h, --help\tprint this message")
      System.exit(0)
    }

    class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
      val source = opt[String](required = true)
      val log = opt[String](required = true)
      val drop = opt[Int](required = true)
      val take = opt[Int](required = true)
      verify()
    }

    def getCLI(args: Array[String]): CLIConf = {
      val cli = new CLIConf(args) {
        override def onError(e: Throwable): Unit = e match {
          case Help("") => help
          case ScallopException(message) => error(message)
          case RequiredOptionNotFound(optionName) => help(optionName)
        }
      }
      
      cli
    }
  }
}