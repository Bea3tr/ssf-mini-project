package ssf.budgetbliss;

import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class AppConfig {

    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.database}")
    private int redisDatabase;

    @Value("${spring.data.redis.username}")
    private String redisUsername;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    private JedisConnectionFactory createJedisFac() {
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        config.setDatabase(redisDatabase);

        if(!redisUsername.trim().equals("")) {
            logger.info("Setting redis username and password");
            config.setUsername(redisUsername);
            config.setPassword(redisPassword);
        }
        JedisClientConfiguration jedisClient = JedisClientConfiguration.builder().build();
        JedisConnectionFactory jedisFac = new JedisConnectionFactory(config, jedisClient);
        jedisFac.afterPropertiesSet();

        return jedisFac;
    }

    @Bean("redis-obj")
    public RedisTemplate<String, Object> createRedisTemplateObj() {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(createJedisFac());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        return template;
    }

    @Bean("redis-string")
    public RedisTemplate<String, String> createRedisTemplate() {

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(createJedisFac());
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        return template;
    }
}
