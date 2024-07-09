FROM ghcr.io/navikt/poao-baseimages/java:17

COPY build/libs/veilarbaktivitet.jar app.jar
