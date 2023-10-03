package org.springframework.data.cockroachdb.it.bank.repository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMetadataRepositoryImpl {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<String> getRegions() {
        return jdbcTemplate.queryForList("select region from [show regions]", String.class);
    }

    public String getGatewayRegion() {
        return jdbcTemplate
                .queryForObject("SELECT gateway_region()", String.class);
    }
}
