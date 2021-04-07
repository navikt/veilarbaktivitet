package no.nav.veilarbaktivitet.service;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import no.nav.veilarbaktivitet.domain.Person;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserInContext {
	private final HttpServletRequest requestProvider;
	private final AuthService authService;

	public Optional<Person.Fnr> getFnr() {
		if (authService.erEksternBruker()) {
			return authService.getInnloggetBrukerIdent().map(Person::fnr);
		}

		Optional<Person> fnr = Optional
			.ofNullable(requestProvider.getParameter("fnr"))
			.map(Person::fnr);

		Optional<Person> aktorId = Optional
			.ofNullable(requestProvider.getParameter("aktorId"))
			.map(Person::aktorId);

		return fnr.or(() -> aktorId).flatMap(this::getFnr);
	}

	private Optional<Person.Fnr> getFnr(Person person) {
		if (person instanceof Person.Fnr) {
			return Optional.of((Person.Fnr) person);
		}

		if (person instanceof Person.AktorId) {
			return authService.getFnrForAktorId((Person.AktorId) person);
		}

		return Optional.empty();
	}
}
