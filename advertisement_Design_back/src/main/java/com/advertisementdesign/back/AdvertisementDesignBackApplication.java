package com.advertisementdesign.back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AdvertisementDesignBackApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdvertisementDesignBackApplication.class, args);
    }
}
