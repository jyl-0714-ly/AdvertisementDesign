package com.advertisementdesign.back;

import com.advertisementdesign.back.consultation.config.ConsultationMatchingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableConfigurationProperties(ConsultationMatchingProperties.class)
@SpringBootApplication
public class AdvertisementDesignBackApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdvertisementDesignBackApplication.class, args);
    }
}
