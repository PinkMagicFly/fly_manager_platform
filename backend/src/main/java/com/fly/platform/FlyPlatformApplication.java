package com.fly.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 翔云·低空智慧物流运营平台 统一后端主启动类
 * 涵盖账户鉴权、资产调度、空域合规、订单运营、在途监测与平台治理六大服务体系
 *
 * @author SoarCloud Team
 * @version 1.0.0
 */
@EnableScheduling
@SpringBootApplication
@MapperScan("com.fly.platform.**.mapper")
public class FlyPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlyPlatformApplication.class, args);
        System.out.println("==========================================================");
        System.out.println("  翔云·低空智慧物流运营平台 (fly_manager_platform) 启动成功!");
        System.out.println("  API 接口文档 Knife4j: http://localhost:8080/doc.html");
        System.out.println("==========================================================");
    }
}
