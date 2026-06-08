FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# JAR compilé depuis la branche websocket (mvn clean package → serveur.jar)
COPY serveur.jar /app/serveur.jar

EXPOSE 8887
CMD ["java", "-jar", "/app/serveur.jar"]
