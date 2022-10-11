package no.nav.veilarbaktivitet.util;

import lombok.NonNull;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class TekstformatteringUtils {
    private TekstformatteringUtils() {
    }

    private static final String MELLOMROM = "\s";
    private static final String BINDESTREK = "-";
    private static final String PARENTES_START = "(";

    public static String storeForbokstaverStedsnavn(@NonNull String sted) {
        return Stream.of(MELLOMROM, BINDESTREK, PARENTES_START)
                .map(TekstformatteringUtils::storeForbokstaverPaOrdMedNoenUnntak)
                .reduce(Function.identity(), Function::andThen)
                .apply(sted.toLowerCase());
    }

    private static Function<String, String> storeForbokstaverPaOrdMedNoenUnntak(String skilletegn) {
        final List<String> ignorer = asList("i", "og");
        return fylke -> Stream.of(fylke.split("[" + skilletegn + "]"))
                .map(s -> ignorer.contains(s) ? s : s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(skilletegn));
    }
}
