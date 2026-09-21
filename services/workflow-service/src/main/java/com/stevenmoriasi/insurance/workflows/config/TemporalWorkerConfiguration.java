package com.stevenmoriasi.insurance.workflows.config;

import com.stevenmoriasi.insurance.workflows.brokers.BrokerOnboardingWorkflowImpl;
import com.stevenmoriasi.insurance.workflows.claims.ClaimActivities;
import com.stevenmoriasi.insurance.workflows.claims.MotorClaimWorkflowImpl;
import com.stevenmoriasi.insurance.workflows.renewals.PolicyRenewalWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
class TemporalWorkerConfiguration {

    @Bean(destroyMethod = "shutdown")
    WorkflowServiceStubs workflowServiceStubs(TemporalProperties properties) {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(properties.target()).build());
    }

    @Bean
    WorkflowClient workflowClient(
            WorkflowServiceStubs serviceStubs, TemporalProperties properties) {
        return WorkflowClient.newInstance(
                serviceStubs,
                WorkflowClientOptions.newBuilder().setNamespace(properties.namespace()).build());
    }

    @Bean(destroyMethod = "shutdown")
    WorkerFactory workerFactory(WorkflowClient workflowClient) {
        return WorkerFactory.newInstance(workflowClient);
    }

    @Bean
    Worker motorClaimWorker(
            WorkerFactory workerFactory,
            TemporalProperties properties,
            ClaimActivities activities) {
        Worker worker = workerFactory.newWorker(properties.taskQueue());
        worker.registerWorkflowImplementationTypes(
                MotorClaimWorkflowImpl.class,
                PolicyRenewalWorkflowImpl.class,
                BrokerOnboardingWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        return worker;
    }

    @Bean
    ApplicationRunner startWorkers(WorkerFactory workerFactory, Worker motorClaimWorker) {
        return arguments -> workerFactory.start();
    }
}
