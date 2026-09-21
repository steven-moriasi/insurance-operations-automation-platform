package com.stevenmoriasi.insurance.integrations.legacy;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

@Configuration
class LegacyDataSourceConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    DataSourceProperties integrationDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    DataSource integrationDataSource(
            @Qualifier("integrationDataSourceProperties")
                    DataSourceProperties integrationDataSourceProperties) {
        return integrationDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    @ConfigurationProperties("insurance.legacy.datasource")
    DataSourceProperties legacyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    DataSource legacyDataSource(
            @Qualifier("legacyDataSourceProperties")
                    DataSourceProperties legacyDataSourceProperties) {
        return legacyDataSourceProperties.initializeDataSourceBuilder().build();
    }

    @Bean
    JdbcClient legacyJdbcClient(@Qualifier("legacyDataSource") DataSource legacyDataSource) {
        return JdbcClient.create(legacyDataSource);
    }
}
