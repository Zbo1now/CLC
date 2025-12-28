package com.campuscoin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.campuscoin.dao")
@EnableScheduling
public class CampuscoinH5Application {

    static {
        // 生成验证码图片时使用 BufferedImage，确保在无图形界面环境下也能运行
        System.setProperty("java.awt.headless", "true");
    }

    public static void main(String[] args) {
        SpringApplication.run(CampuscoinH5Application.class, args);
    }
}
