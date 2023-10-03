package org.springframework.data.cockroachdb.shell;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.CompletionContext;
import org.springframework.shell.CompletionProposal;
import org.springframework.shell.standard.ValueProvider;

public class ClusterSettingProvider implements ValueProvider {
    @Autowired
    private Query query;

    @Override
    public List<CompletionProposal> complete(CompletionContext completionContext) {
        List<CompletionProposal> result = new ArrayList<>();

        JdbcTemplate jdbcTemplate = query.getJdbcTemplate();
        jdbcTemplate.query("select * from [SHOW ALL CLUSTER SETTINGS]", rs -> {
            // variable
            // value
            // setting_type
            // public

            String prefix = completionContext.currentWordUpToCursor();
            if (prefix == null) {
                prefix = "";
            }
            String name = rs.getString(1);
            if (name.startsWith(prefix)) {
                result.add(new CompletionProposal(name));
            }
        });

        return result;
    }
}
