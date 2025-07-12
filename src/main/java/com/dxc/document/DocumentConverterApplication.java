package com.dxc.document;

import org.apache.catalina.Context;
import org.apache.tomcat.util.http.fileupload.FileUploadBase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@SpringBootApplication
public class DocumentConverterApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentConverterApplication.class, args);
	}

	
	  @Bean
	    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
	        return factory -> factory.addConnectorCustomizers(connector -> {
	            // ✅ Allows more form fields (file parts)
	            connector.setProperty("fileCountMax", "1000");       // actual fix for file count
	            connector.setProperty("maxParameterCount", "10000"); // optional, handles more fields
	            connector.setMaxPostSize(209715200); // 200MB
	        });
	    }
	  
	  @Bean
	    public WebMvcConfigurer corsConfigurer() {
	        return new WebMvcConfigurer() {
	            @Override
	            public void addCorsMappings(CorsRegistry registry) {
	                registry.addMapping("/**")
	                        .allowedOrigins("*")
	                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
	                        .allowedHeaders("*");
	            }
	        };
	    }	
}
