//package com.dxc.document;
//
//import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
//import org.springframework.boot.web.server.WebServerFactoryCustomizer;
//import org.springframework.boot.web.servlet.MultipartConfigFactory;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.unit.DataSize;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//import jakarta.servlet.MultipartConfigElement;
//
//
//@Configuration
//public class Webconfig {
//	
//	  @Bean
//	    public WebMvcConfigurer corsConfigurer() {
//	        return new WebMvcConfigurer() {
//	            @Override
//	            public void addCorsMappings(CorsRegistry registry) {
//	                registry.addMapping("/**")
//	                        .allowedOrigins("*")
//	                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//	                        .allowedHeaders("*");
//	            }
//	        };
//	    }
//
//	  
//	  @Bean
//	    public MultipartConfigElement multipartConfigElement() {
//	        MultipartConfigFactory factory = new MultipartConfigFactory();
//	        
//	        factory.setMaxFileSize(DataSize.ofMegabytes(50));        // Max size per file
//	        factory.setMaxRequestSize(DataSize.ofMegabytes(200));    // Max total size
//	        factory.setFileSizeThreshold(DataSize.ofBytes(0));       // No in-memory buffering
//
//	        return factory.createMultipartConfig();
//	    }
//
//	  
//	
//	  
//}
