package no.nav.veilarbaktivitet.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class EnumUtils {

    public static String getName(Enum<?> anEnum){
        return anEnum != null ? anEnum.name() : null;
    }

    public static <T extends Enum> T valueOf(Class<T> enumClass, String name) {
        var enumValue = Arrays.stream(enumClass.getEnumConstants())
                .filter(e -> e.name().equals(name))
                .findAny()
                .orElse(null);
        if (enumValue == null) {
            log.warn("Kunne ikke deserialisere arena-status {}", name);
        }
        return enumValue;
    }

}
