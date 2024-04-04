package no.nav.veilarbaktivitet.annotation_test

import TestWebMvcConfigurer
import org.springframework.context.annotation.Import
import java.lang.annotation.Inherited

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Import(TestWebMvcConfigurer::class)
annotation class EnableInterceptor()