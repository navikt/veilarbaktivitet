import no.nav.veilarbaktivitet.annotation_test.TestInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

//@Configuration
//open class TestInterceptorConfiguration() {
//    @Bean
//    open fun testWebMvcConfigurer(testInterceptor: TestInterceptor): TestWebMvcConfigurer {
//        return TestWebMvcConfigurer(testInterceptor)
//    }
//}

@Configuration
open class TestWebMvcConfigurer(val testInterceptor: TestInterceptor) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(testInterceptor)
    }
}