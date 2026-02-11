package com.group17.lilyoutube_server.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JpaAliasConfig {

    @Bean(name = "jpaSharedEM_entityManagerFactory")
    public jakarta.persistence.EntityManager jpaSharedEM_entityManagerFactory(@org.springframework.context.annotation.Lazy EntityManagerFactory entityManagerFactory) {
        return org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

}
