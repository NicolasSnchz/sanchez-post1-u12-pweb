# Dockerfile multi-stage: compilar con JDK + Maven y ejecutar solo con el JRE

# -- Etapa 1: compilacion con Maven ------------------------------------------------------------
# La guia usa eclipse-temurin:21-jdk-alpine, que no trae Maven: "RUN mvn ..." fallaria con
# "mvn: not found". Esta imagen oficial es el mismo JDK 21 (Temurin, Alpine) con Maven 3.9.9
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# pom.xml primero para aprovechar la cache de capas: si las dependencias no cambian, esta capa se
# reutiliza y Maven no las vuelve a descargar en los builds siguientes
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

COPY src ./src
RUN mvn -B clean package -DskipTests -q

# -- Etapa 2: imagen de ejecucion (solo JRE) ---------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Usuario no root, duenio de /app para que el perfil dev pueda escribir logs/pedidos.log
RUN addgroup -S spring && adduser -S spring -G spring && chown spring:spring /app
USER spring

# Solo el JAR compilado de la etapa anterior: ni Maven, ni el codigo fuente, ni el JDK
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# MaxRAMPercentage: el heap usa la mitad de la memoria del contenedor (el plan gratuito de Render da
# 512 MB); sin el, la JVM se limita al 25 %
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=50", "-jar", "app.jar"]
