package com.kanban;

import com.kanban.auth.config.JwtProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class KanbanApplication {

    public static void main(String[] args) {
        // .env 파일 로딩 (모노레포 루트 디렉토리에서 찾음)
        // backend 폴더에서 실행되므로 상위 디렉토리(..)에서 .env 파일을 찾음
        Dotenv dotenv = Dotenv.configure()
                .directory("..")  // 모노레포 루트 디렉토리 (backend의 상위 폴더)
                .ignoreIfMissing()  // .env 파일이 없어도 에러 발생 안함
                .load();

        // .env의 모든 환경변수를 시스템 속성으로 설정
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });

        SpringApplication.run(KanbanApplication.class, args);
    }

}
