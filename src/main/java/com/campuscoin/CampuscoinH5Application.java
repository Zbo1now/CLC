package com.campuscoin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campuscoin.dao")
public class CampuscoinH5Application {
    public static void main(String[] args) {
        SpringApplication.run(CampuscoinH5Application.class, args);
    }
}
