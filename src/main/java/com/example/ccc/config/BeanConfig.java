package com.example.ccc.config;
import com.example.ccc.component.ScoreCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {

    @Bean // ✅ 手动注册 Bean
    public ScoreCalculator scoreCalculator() {
        return new ScoreCalculator();
    }
}
