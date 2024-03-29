package com.xiaoyuan.back;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

/**
 * FileName:    BackApplication
 * Author:      小袁
 * Date:        2022/4/15 10:39
 * Description:
 */
@SpringBootApplication(scanBasePackages = {"com.xiaoyuan"})
@ComponentScan({"com.xiaoyuan"})
@MapperScan({"com.xiaoyuan.back.mapper"})
@EnableAsync
@EnableScheduling
public class BackApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackApplication.class, args);
        System.out.println("========================== 小袁博客后台管理平台启动成功 ==========================");
    }

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }
}
