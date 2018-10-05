# aspace top container update tool

usage: java -jar TCUpdate.jar [options]<br>
  options:<br>
    -s, --source, required	path to csv file to be input<br>
    -d, --drop, required	number of rows to skip from the beginning of csv file<br>
    -t, --take, required	number of rows to process from csv file<br>
    -h, --help	print this message<br>
    
* prerequisites
[sbt](https://www.scala-sbt.org/), built with v0.13.13<br/>
on osx: brew install sbt<br/>
on linux: http://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html<br/>

* Build<br>
$ git clone https://github.com/NYULibraries/aspace_topcontainer_updater<br/>
$ cd aspace_topcontainer_updater<br/>
$ cp src/main/resources/application.conf_template src/main/resources/application.conf<br />
$ vi src/main/resources/application.conf #Enter your aspace environments and save<br/>
$ sbt assembly #this will build an executable jar<br/>
$ cp target/scala-2.12/TCUpdate.jar .<br>
$ java -jar TCUpdate.jar --help
