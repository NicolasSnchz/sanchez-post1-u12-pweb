# Post-contenido, Unidad 12: Despliegue y CI/CD

**Estudiante:** Nicolás Andrés Sánchez Villamizar · Programación Web · Universidad de Santander (UDES)

## Descripción

Repositorio del laboratorio de la Unidad 12 de Programación Web. Contiene la aplicación Spring Boot de pedidos de la Unidad 11 (API REST `/api/pedidos` con la entidad JPA `Pedido`, patrón Strategy para el descuento, manejo de errores y Swagger), dockerizada con un Dockerfile multi-stage y perfiles `dev`/`prod` (Parte 1). Se extenderá con un pipeline de integración continua en GitHub Actions y monitoreo con Spring Boot Actuator (Parte 2).

## Parte 1: Dockerización con perfiles y variables de entorno

El [`Dockerfile`](Dockerfile) tiene dos etapas:

1. **Compilación:** compila con Maven sobre JDK 21 (Temurin, Alpine).
2. **Ejecución:** corre el JAR sobre el JRE 21 Alpine, con un usuario no root.

La configuración sensible está en [`application-dev.properties`](src/main/resources/application-dev.properties) y [`application-prod.properties`](src/main/resources/application-prod.properties), siempre como `${VARIABLE:valor-por-defecto}`. [`application.properties`](src/main/resources/application.properties) solo tiene lo común y elige el perfil con `SPRING_PROFILES_ACTIVE` (por defecto `dev`):

- **Sin variables de entorno:** perfil `dev`, con H2 en memoria.
- **Con `SPRING_PROFILES_ACTIVE=prod` y las variables de la base:** perfil `prod`, con PostgreSQL.

| Variable | Perfil | Por defecto | Para qué |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | ambos | `dev` | Elige el perfil |
| `PORT` | ambos | `8080` | Puerto HTTP (Render lo fija solo) |
| `DATASOURCE_URL` | dev / prod | `jdbc:h2:mem:devdb` / `jdbc:postgresql://localhost:5432/appdb` | URL JDBC de la base |
| `DATASOURCE_USER` | dev / prod | `sa` / `appuser` | Usuario de la base |
| `DATASOURCE_PASSWORD` | dev / prod | vacía | Contraseña (en prod siempre debe llegar por variable) |
| `DATASOURCE_DRIVER` | dev | `org.h2.Driver` | Driver JDBC en dev; en prod lo deduce de la URL |

El proyecto no usa JWT, así que no necesita `JWT_SECRET`.

## Cómo construir y ejecutar

1. Clonar: `git clone https://github.com/NicolasSnchz/sanchez-post1-u12-pweb.git`
2. Compilar sin Docker (opcional): `./mvnw clean package -DskipTests` y luego `java -jar target/pedidos-0.0.1-SNAPSHOT.jar`
3. Construir la imagen: `docker build -t mi-spring-app:local .`
4. Ejecutar con los valores por defecto (perfil dev):
   ```
   docker run -d --name mi-app-dev -p 8080:8080 mi-spring-app:local
   curl http://localhost:8080/api/pedidos?clienteEmail=ana@correo.com
   ```
5. Ejecutar con el perfil prod, contra un PostgreSQL:
   ```
   docker run -d --name mi-app-prod -p 8080:8080 \
     -e SPRING_PROFILES_ACTIVE=prod \
     -e DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/appdb \
     -e DATASOURCE_USER=appuser -e DATASOURCE_PASSWORD=apppass \
     mi-spring-app:local
   docker logs mi-app-prod     # debe decir: The following 1 profile is active: "prod"
   ```

## Decisiones de diseño (Parte 1)

1. **Placeholders con valor por defecto (`${VAR:default}`) en vez de placeholders estrictos.**
   - Un placeholder estricto obliga a exportar todas las variables incluso para correr en local o en las pruebas del pipeline, y falla con `PlaceholderResolutionException` si falta una.
   - Con valores por defecto seguros para desarrollo (H2 en memoria), el mismo artefacto arranca sin configurar nada, y en producción la plataforma sobrescribe cada valor.
   - La contraseña de producción no tiene un valor por defecto utilizable.
2. **`application-dev.properties` y `application-prod.properties` como archivos separados.**
   - El nombre del archivo dice a qué ambiente pertenece cada valor.
   - Así es más difícil cambiar por error una propiedad de producción mientras se trabaja en desarrollo.
3. **Imagen final `eclipse-temurin:21-jre-alpine` en vez del JDK completo o de distroless.**
   - Alpine con solo el JRE quita el compilador y las herramientas de build.
   - A diferencia de distroless, conserva un shell mínimo para depurar con `docker exec -it`.
4. **Etapa de compilación con la imagen oficial `maven:3.9.9-eclipse-temurin-21-alpine`.**
   - La guía usa `eclipse-temurin:21-jdk-alpine`, que no trae Maven: `RUN mvn ...` fallaría.
   - La imagen oficial de Maven es el mismo JDK 21 Temurin sobre Alpine, con Maven instalado.
5. **Esquema de producción con un script idempotente.**
   - El perfil `prod` usa `ddl-auto=validate`, que no crea tablas: con una base nueva en Render la aplicación no arrancaría.
   - [`db/schema-postgresql.sql`](src/main/resources/db/schema-postgresql.sql) (`CREATE TABLE IF NOT EXISTS`) crea la tabla antes de que Hibernate la valide.
   - El driver de PostgreSQL, que la guía no agrega, está en el `pom.xml` como dependencia de ejecución.
