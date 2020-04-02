FROM openjdk:8

RUN mkdir app

ADD target/scala-2.12/cluster-planner-server.jar app/cluster-planner-server.jar

ENV PORT 9090

WORKDIR app

ENTRYPOINT ["java", "-jar", "cluster-planner-server.jar"]

EXPOSE 9090
