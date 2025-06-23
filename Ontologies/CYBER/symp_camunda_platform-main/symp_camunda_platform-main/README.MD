# SyMP Camunda Platform

[![Compatible with Camunda Modeler version 3.3](https://img.shields.io/badge/Camunda%20Modeler-3.3+-blue.svg)](https://github.com/camunda/camunda-modeler)
[![license](https://img.shields.io/badge/license-MIT-green.svg)](https://opensource.org/licenses/MIT)


This research implementation of the [Camunda Platform](https://docs.camunda.org) extends the Platform with basic workers for uploading an ontology, merging them into a single model, providing SWRL and Jena rule engines and remerging model versions processed in parallel.


## Eclipse
To be able to run this project in Eclipse jre 12 or higher is requiered. Moreover, no module-info.java is supposed to be set and the build path needs to be adapted to not contain the jre system library within the module path but within the class path.
## Running the Server

There will be a dockerized version of the whole SyMP implementation (including all components), which will follow soon. Meanwhile, the components have to be built and started seperatelly.

To run this servlet inside an Apache Tomcat Server on your local development machine, you first need to clone https://gitlab-ext.iosb.fraunhofer.de/betnik/tomcat-camunda. The Tomcat Project on Gitlab provides all required Camunda 
dependencies and initializes all basic Camunda Modules such as Cockpit or Tasklist.

After this you have to move the `/target/*.war` file, which you get from running `mvn install` to `%CATALINA_HOME%/webapps/*.war`. `%CATALINA_HOME%` describes the root directory of your Tomcat Server folder. 


# Disclaimer
The software has partially been developed within a research project funded by the German Federal Ministry of Education and Research (BMBF).


## Contributors
* Florian Patzer <florian.patzer@iosb.fraunhofer.de>
* Niklas Betten
