package no.nav.veilarbaktivitet.util;

import static no.nav.common.utils.EnvironmentUtils.isDevelopment;

public class EnvironmentUtils {

    private EnvironmentUtils() {}

    public static String scope(String appName, String namespace, String cluster) {
        return String.format("api://%s.%s.%s/.default", cluster, namespace, appName);
    }

    public static Boolean isDev() {
        return isDevelopment().orElse(false);
    }

}
