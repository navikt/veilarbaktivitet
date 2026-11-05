FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21

ENV TZ="Europe/Oslo"
WORKDIR /app
#COPY nais/init.sh /init-scripts/init.sh
COPY build/libs/veilarbaktivitet.jar ./
EXPOSE 8080
USER nonroot
CMD ["-jar", "veilarbaktivitet.jar"]
