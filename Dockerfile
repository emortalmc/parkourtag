FROM eclipse-temurin:17-jre-alpine

RUN mkdir /app
WORKDIR /app

COPY build/libs/*-all.jar /app/parkour_tag.jar
COPY run/maps/*.tnt /app/maps/

CMD ["java", "-jar", "/app/parkour_tag.jar"]