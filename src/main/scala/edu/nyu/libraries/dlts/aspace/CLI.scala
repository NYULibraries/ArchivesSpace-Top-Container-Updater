package edu.nyu.libraries.dlts.aspace

import java.io.File
import java.net.URL

import com.typesafe.config.ConfigFactory
import org.rogach.scallop.exceptions.{Help, RequiredOptionNotFound, ScallopException}
import org.rogach.scallop.{ScallopConf, ScallopOption}

import scala.io.StdIn

object CLI {

	trait CLISupport {

    private val conf = ConfigFactory.load()

    def help(optionName: String) {
      println(s"Error: Missing required option $optionName")
      help()
    }

	    def error(message: String) {
      println(message)
      println(help())
    }

    def help(): Unit = {
      println("usage: java -jar TCUpdate.jar [options]")
      println("  options:")
      println("    -s, --source, required\tpath to csv file to be input")
      println("    -d, --drop, optional\tnumber of rows to skip from the beginning of csv file")
      println("    -t, --take, optional\tnumber of rows to process from csv file")
      println("    -h, --help\tprint this message")
      System.exit(0)
    }

    class CLIConf(arguments: Seq[String]) extends ScallopConf(arguments) {
      val source: ScallopOption[String] = opt[String](required = true)
      val drop: ScallopOption[Int] = opt[Int](required = false)
      val take: ScallopOption[Int] = opt[Int](required = false)
      verify()
    }

    def getAspaceOptions(args: Array[String]): AspaceSession = {

      val cli = new CLIConf(args) {
        override def onError(e: Throwable): Unit = e match {
          case Help("") => help()
          case ScallopException(message) => error(message)
          case RequiredOptionNotFound(optionName) => help(optionName)
        }
      }

      val source: File = new File(cli.source.toOption.get)
      dialogue(AspaceSession(None, None, None, source, conf.getInt("client.timeout"), cli.drop.toOption, cli.take.toOption, None, None))
    }


    private def dialogue(aspaceSession: AspaceSession): AspaceSession = {
      val env = Some(new URL(conf.getString(s"client.$getEnv")))
      val usr = getFromConsole("username")
      val pswd = getFromConsole("password")

      aspaceSession.copy(username = Some(usr), password= Some(pswd), url = env)
    }

    private def getEnv: String = {
      print("Enter the environment (prod, stage, dev), or q to quit: ")
      val env = StdIn.readLine.toLowerCase.trim

      env == "q" match {
        case true => System.exit(0) //quit the program
        case false =>
      }

      env match {
        case "prod" => env
        case "stage" => env
        case "dev" => env
        case _ => {
          println("Input is not a valid environment")
          getEnv
        }
      }

    }

    private def getFromConsole(str: String): String = {
      print(s"Enter the $str, or q to quit: ")
      val in = StdIn.readLine.trim
      in == "q" match {
        case true => System.exit(0) //quit the program
        case false =>
      }

      in
    }
	}
}
