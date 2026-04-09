package com.kritik.POS.common.config;

import com.kritik.POS.common.route.FileRoute;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath().normalize();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(FileRoute.UPLOADS_RESOURCE_PATTERN)
                .addResourceLocations(UPLOAD_DIR.toUri().toString());
    }
}
