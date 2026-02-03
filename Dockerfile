FROM busybox:1.36.1-uclibc as busybox

FROM gcr.io/distroless/java21

COPY --from=busybox /bin/sh /bin/sh
COPY --from=busybox /bin/printenv /bin/printenv
COPY --from=busybox /bin/mkdir /bin/mkdir
COPY --from=busybox /bin/chown /bin/chown

ENV TZ="Europe/Oslo"
WORKDIR /app
#COPY nais/init.sh /init-scripts/init.sh
COPY build/libs/veilarbaktivitet.jar ./
EXPOSE 8080
USER nonroot
CMD ["veilarbaktivitet.jar"]
