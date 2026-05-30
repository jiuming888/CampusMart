package com.campusmart;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campusmart.mapper")
public class CampusMartApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusMartApplication.class, args);
    }
}
