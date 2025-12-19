package no.nav.veilarbaktivitet.person;

public record EksternBruker(
        Person.Fnr fnr,
        Person.AktorId aktorId
) {}
