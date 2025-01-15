FROM openjdk:11
RUN mkdir /usr/executables
COPY target/grpcServerApp-1.0-jar-with-dependencies.jar /usr/executables
RUN mkdir /usr/datafiles
WORKDIR /usr/datafiles
ENTRYPOINT ["java", "-jar", "/usr/executables/grpcServerApp-1.0-jar-with-dependencies.jar"]
