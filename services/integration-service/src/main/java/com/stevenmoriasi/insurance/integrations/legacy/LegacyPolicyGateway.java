package com.stevenmoriasi.insurance.integrations.legacy;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public class LegacyPolicyGateway {

    private final JdbcClient legacyDatabase;

    public LegacyPolicyGateway(JdbcClient legacyJdbcClient) {
        this.legacyDatabase = legacyJdbcClient;
    }

    public Optional<LegacyPolicy> findByPolicyNumber(String policyNumber) {
        return legacyDatabase
                .sql(
                        """
                        select policy_number, product_code, status, cover_start_date,
                               cover_end_date, currency
                        from legacy_policy
                        where policy_number = :policyNumber
                        """)
                .param("policyNumber", policyNumber)
                .query(
                        (resultSet, rowNumber) ->
                                new LegacyPolicy(
                                        resultSet.getString("policy_number"),
                                        resultSet.getString("product_code"),
                                        resultSet.getString("status"),
                                        resultSet.getObject("cover_start_date", LocalDate.class),
                                        resultSet.getObject("cover_end_date", LocalDate.class),
                                        resultSet.getString("currency")))
                .optional();
    }

    public record LegacyPolicy(
            String policyNumber,
            String productCode,
            String status,
            LocalDate coverStartDate,
            LocalDate coverEndDate,
            String currency) {}
}
