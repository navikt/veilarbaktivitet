package no.nav.fo.veilarbaktivitet.util;

import java.util.function.*;

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

    public static <A, B> BiConsumerComposer<A, B> kall(BiConsumer<A, B> biConsumer) {
        return new BiConsumerComposer<>(biConsumer);
    }

    public static class BiConsumerComposer<VENSTRE, HOYRE> {

        private final BiConsumer<VENSTRE, HOYRE> biConsumer;

        private BiConsumerComposer(BiConsumer<VENSTRE, HOYRE> biConsumer) {
            this.biConsumer = biConsumer;
        }

        public ConsumerComposer<HOYRE> med(VENSTRE venstre) {
            return new ConsumerComposer<>(hoyre -> {
                if (venstre != null && hoyre != null) {
                    biConsumer.accept(venstre, hoyre);
                }
            });
        }
    }

    public static class ConsumerComposer<T> {

        private final Consumer<T> consumer;

        private ConsumerComposer(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        public void og(T t) {
            if (t != null) {
                consumer.accept(t);
            }
        }
    }

}
