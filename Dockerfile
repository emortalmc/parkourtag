FROM azul/zulu-openjdk:25-jre

RUN mkdir /app
WORKDIR /app

# Download packages
RUN apt-get update && apt-get install -y wget

COPY build/libs/*-all.jar /app/parkourtag.jar
COPY run/maps /app/maps

ENTRYPOINT ["java"]
CMD ["-jar", "/app/parkourtag.jar"]
