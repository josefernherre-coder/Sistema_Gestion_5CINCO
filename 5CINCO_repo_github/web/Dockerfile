# Imagen para publicar la demo web en Render (u otro servicio con Docker)
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY lib lib
COPY src src
RUN mkdir out && javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name '*.java')

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/out out
COPY lib lib
COPY demo demo
ENV PORT=10000
EXPOSE 10000
CMD ["java", "-Xmx300m", "-cp", "out:lib/*", "web.Main"]
