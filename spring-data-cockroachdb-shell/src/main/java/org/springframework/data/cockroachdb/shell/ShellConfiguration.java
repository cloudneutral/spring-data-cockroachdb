package org.springframework.data.cockroachdb.shell;

import org.jline.reader.LineReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.shell.Shell;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.jline.PromptProvider;

import org.springframework.data.cockroachdb.shell.support.HybridShellRunner;

@Configuration
public class ShellConfiguration {
    @Bean
    public HybridShellRunner hybridShellRunner(LineReader lineReader,
                                               PromptProvider promptProvider,
                                               Shell shell,
                                               ShellContext shellContext) {
        return new HybridShellRunner(lineReader, promptProvider, shell, shellContext);
    }

    @Bean
    public ClusterSettingProvider clusterSettingProvider() {
        return new ClusterSettingProvider();
    }
}
