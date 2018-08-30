# aspace top container update tool

usage: java -jar TCUpdate.jar [options]<br>
  options:<br>
    -s, --source, required	path to csv file to be input<br>
    -l, --log, required		path to log file to be written<br>
    -d, --drop, required	number of rows to skip from the beginning of csv file<br>
    -t, --take, required	number of rows to process from csv file<br>
    -h, --help	print this message<br>
    
* prerequisites
[sbt](https://www.scala-sbt.org/), built with v0.13.13

* Build<br>
$ git clone https://github.com/NYULibraries/aspace_topcontainer_updater<br/>
$ cd aspace_topcontainer_updater<br/>
$ vi src/main/resources/application.conf #Enter your aspace credentials and save<br/>
$ sbt assembly #this will build an executable jar<br/>
$ cp target/scala-2.12/TCUpdate.jar .<br>
$ java -jar TCUpdate.jar --help
