package edu.nyu.libraries.dlts.aspace

import java.net.URL

import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.DefaultFormats
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.{HttpGet, HttpPost}
import org.apache.http.impl.client.{CloseableHttpClient, HttpClientBuilder}
import org.apache.http.util.EntityUtils
import org.apache.http.entity.StringEntity

import scala.io.Source

object AspaceClient {

  trait AspaceSupport {

    val header = "X-ArchivesSpace-Session"

    implicit val formats = DefaultFormats

    def getSession(session: AspaceSession): AspaceSession = {
      val client = getClient(session.timeout)
      val token = getToken(session.username.get, session.password.get, session.url.get, client)
      session.copy(client = Some(client), token = token)
    }

    private def getClient(timeout: Int): CloseableHttpClient = {
      val config = RequestConfig.custom()
        .setConnectTimeout(timeout * 1000)
        .setConnectionRequestTimeout(timeout * 1000)
        .setSocketTimeout(timeout * 1000).build();

      HttpClientBuilder.create().setDefaultRequestConfig(config).build()
    }

    private def getToken(user: String, password: String, url: URL, client: CloseableHttpClient): Option[String] = {
      try {
        val tokenRequest = new HttpPost(url.toString + s"/users/$user/login?password=$password")
        val response = client.execute(tokenRequest)
        response.getStatusLine.getStatusCode match {
          case 200 => {
            val entity =response.getEntity
            val content = entity.getContent
            val data = Source.fromInputStream(content).mkString
            val json = parse(data)
            val token = (json \ "session").extract[String]
            EntityUtils.consume(entity)
            response.close()
            Some(token)
          }
          case _ => None
        }
      } catch {
        case e: Exception => { None }
      }

    }

    def get(session: AspaceSession, aspace_url: String): Option[JValue] = {
      try {
        val httpGet = new HttpGet(session.url.get.toString + aspace_url)
        httpGet.addHeader(header, session.token.get)
        val response = session.client.get.execute(httpGet)
        val entity = response.getEntity
        val content = entity.getContent
        val data = scala.io.Source.fromInputStream(content).mkString
        EntityUtils.consume(entity)
        response.close
        Some(parse(data))
      } catch {
        case e: Exception =>  { None }
      }
    }

    def postJson(session: AspaceSession, aoUrl: String, data: String): Option[Int] = {
      try {
        val httpPost = new HttpPost(session.url.get.toString + aoUrl)
        httpPost.addHeader(header, session.token.get)
        val entity = new StringEntity(data, "UTF-8")
        httpPost.setEntity(entity)
        httpPost.setHeader("Content-type", "application/json; charset=UTF-8")
        val response = session.client.get.execute(httpPost)
        val code = response.getStatusLine().getStatusCode().toInt
        EntityUtils.consume(entity)
        response.close
        Some(code)
      } catch {
        case e: Exception => {
          println("HI")
          None
        }
      }
    }
  }

}
