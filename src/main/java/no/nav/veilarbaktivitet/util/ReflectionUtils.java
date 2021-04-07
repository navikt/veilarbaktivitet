package no.nav.veilarbaktivitet.util;

import java.lang.reflect.Method;
import lombok.SneakyThrows;

public class ReflectionUtils {

	@SneakyThrows
	public static Method getMethod(
		Class<?> proxyClass,
		String methodName,
		Class<?>... args
	) {
		return proxyClass.getMethod(methodName, args);
	}
}
