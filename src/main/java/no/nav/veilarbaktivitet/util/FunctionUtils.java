package no.nav.veilarbaktivitet.util;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Optional.ofNullable;

public class FunctionUtils {

    public static <T> Supplier<T> supply(T value) {
        return () -> value;
    }

    public static <A, TO> Supplier<TO> applyFunction(Supplier<A> supplier, Function<A, TO> mapper) {
        return () -> ofNullable(supplier)
                .map(Supplier::get)
                .map(mapper)
                .orElse(null);
    }

    public static <A, B> BiConsumer<A, B> nullSafe(BiConsumer<A, B> biConsumer) {
        return (a, b) -> {
            if (a != null && b != null) {
                biConsumer.accept(a, b);
            }
        };
    }

    public static <A, B, C> TriConsumer<A, B, C> nullSafe(TriConsumer<A, B, C> triConsumer) {
        return (a, b, c) -> {
            if (a != null && b != null && c != null) {
                triConsumer.accept(a, b, c);
            }
        };
    }

    public interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);
    }

}
