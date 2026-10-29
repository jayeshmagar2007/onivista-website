FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . .
RUN javac OnivistaAgroServer.java
EXPOSE 10000
CMD ["sh", "-c", "java OnivistaAgroServer"]
