package no.nav.fo.veilarbaktivitet.util;

import java.util.function.*;

import static no.nav.fo.veilarbaktivitet.util.FunctionUtils.applyFunction;
import static no.nav.fo.veilarbaktivitet.util.FunctionUtils.supply;

public class MappingUtils {

    public static <A> Merger<A> merge(A venstre, A hoyre) {
        return new Merger<>(supply(venstre), supply(hoyre));
    }

    public static class Merger<T> {
        private final Supplier<T> venstreSupplier;
        private final Supplier<T> hoyreSupplier;

        private Merger(Supplier<T> venstreSupplier, Supplier<T> hoyreSupplier) {
            this.venstreSupplier = venstreSupplier;
            this.hoyreSupplier = hoyreSupplier;
        }

        public <U> Merger<U> map(Function<T, U> mappingFunksjon) {
            return new Merger<>(
                    applyFunction(venstreSupplier, mappingFunksjon),
                    applyFunction(hoyreSupplier, mappingFunksjon)
            );
        }

        // evt reduce() ?
        public T merge(BiFunction<T, T, T> merger) {
            T venstre = venstreSupplier.get();
            T hoyre = hoyreSupplier.get();
            if (venstre != null && hoyre != null) {
                return merger.apply(venstre, hoyre);
            } else {
                return null;
            }
        }
    }

}
