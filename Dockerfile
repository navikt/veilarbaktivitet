FROM docker.adeo.no:5000/bekkci/maven-builder as maven-build

# brukes av testene
ARG testmiljo
ARG domenebrukernavn
ARG domenepassord

ADD / /source
RUN build /source

FROM docker.adeo.no:5000/bekkci/skya-deployer as deployer
COPY --from=maven-build /source /deploy

FROM docker.adeo.no:5000/bekkci/backend-smoketest as smoketest

# TODO oppsett for nais
