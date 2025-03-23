FROM azul/zulu-openjdk:21-jre

RUN mkdir /app
WORKDIR /app

# Download packages
RUN apt-get update && apt-get install -y wget

COPY build/libs/*-all.jar /app/parkourtag.jar
COPY run/maps /app/maps
COPY run/natives /app/natives

ENTRYPOINT ["java"]
CMD ["-jar", "/app/parkourtag.jar"]
