package com.example.ShopFlow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class RedisConfigTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @Test
    @DisplayName("Deve realizar operacoes de SET, GET e DELETE no Redis com sucesso")
    void deveValidarConexaoComRedis(){

        //Arrange
        String key = "shopflow:test:ping";
        String expectedValue = "Pong Redis OK!";

        //ACT
        redisTemplate.opsForValue().set(key, expectedValue, Duration.ofSeconds(60));

        Object actValue = redisTemplate.opsForValue().get(key);


        //ASSERT
        assertThat(actValue).isNotNull();
        assertThat(actValue.toString()).isEqualTo(expectedValue);

        //clean
        Boolean deleted = redisTemplate.delete(key);
        assertThat(deleted).isTrue();
    }

}