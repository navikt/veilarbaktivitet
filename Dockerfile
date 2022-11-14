FROM ghcr.io/navikt/pus-nais-java-app/pus-nais-java-app:java17

COPY nais/init.sh /init-scripts/init.sh
COPY target/veilarbaktivitet.jar app.jar
