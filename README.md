# aspace top container update tool

usage: java -jar TCUpdate.jar [options]<br>
  options:<br>
    -s, --source, required,	path to csv file to be input<br>
    -d, --drop, optional,	number of rows to skip from the beginning of csv file<br>
    -t, --take, optional,	number of rows to process from csv file<br>
    -h, --help	print this message<br>
    
Prerequisites
=============
[sbt](https://www.scala-sbt.org/), built with v0.13.13<br/>
on osx: brew install sbt<br/>
on linux: http://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html<br/>

Build
=====
```
$ git clone https://github.com/NYULibraries/aspace_topcontainer_updater
$ cd aspace_topcontainer_updater
$ cp src/main/resources/application.conf_template src/main/resources/application.conf
$ vi src/main/resources/application.conf #Enter your aspace environments and save
$ sbt assembly #this will build an executable jar
$ cp target/scala-2.12/TCUpdate.jar .
$ java -jar TCUpdate.jar --help
```

Run
===
The application takes a csv file container 3 columns: the url of the archival object you want updated, the url of its current top container, and the url of the target top container.

example row:<br /> 
/repositories/2/archival_objects/113879,/repositories/2/top_containers/17218,/repositories/2/top_containers/17219<br />

The csv file can be process by passing it to the application `$ java -jar TCUpdate.jar -s mycsv.csv`
This will generate a log file of the updates and an error log if any failures occur.
