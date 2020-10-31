# jacoco-server
Jacoco TCP Socket server multi-client Coverage collection.
This repo contains a JaCoCo Server that receives the reports from all the running apps connected to it via a TCP socket.

This will run the JaCoCo server:

```shell script
$jacoco-server> mvn clean package
$jacoco-server> java -jar target/jacoco-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

To play with the server, please clone the other repo https://github.com/xpepper/coverage-test