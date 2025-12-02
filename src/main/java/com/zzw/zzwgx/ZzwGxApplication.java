package com.zzw.zzwgx;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zzw.zzwgx.mapper")
public class ZzwGxApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZzwGxApplication.class, args);
    }

}
