package no.nav.veilarbaktivitet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CustomWebMvcConfigurer implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/internal/kassering/dialog").setViewName("redirect:/internal/kassering/dialog/");
        registry.addViewController("/internal/kassering/dialog/").setViewName("forward:/internal/kassering/dialog/index.html");

        registry.addViewController("/internal/kassering").setViewName("redirect:/internal/kassering/");
        registry.addViewController("/internal/kassering/").setViewName("forward:/internal/kassering/index.html");
    }
}
