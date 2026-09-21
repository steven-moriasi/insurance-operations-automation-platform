package com.stevenmoriasi.insurance.workflows.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("insurance.temporal")
public record TemporalProperties(String target, String namespace, String taskQueue) {}
