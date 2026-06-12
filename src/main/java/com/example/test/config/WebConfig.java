package com.example.test.config;



import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Configuration
// CRITICAL: Tells Spring to convert PageImpl to a stable PagedModel DTO layout dynamically before rendering JSON
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
public class WebConfig {
    // You can leave this empty; the annotation does all the work globally!
}
