# 1. On télécharge un environnement Java 17 très léger
FROM eclipse-temurin:17-jre-alpine

# 2. On définit le dossier de travail à l'intérieur du conteneur
WORKDIR /app

# 3. On copie le serveur.jar du NAS vers le conteneur
COPY serveur.jar /app/serveur.jar

# 4. On indique quel port ouvrir (celui que tu as mis dans ton code Java)
EXPOSE 8887

# 5. La commande à taper pour lancer le serveur
CMD ["java", "-jar", "serveur.jar"]