FROM openjdk:8-jdk
RUN apt-get update && apt-get install -y \
	sudo \
	xfsprogs \
	&& rm -rf /var/lib/apt/lists/*

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]
