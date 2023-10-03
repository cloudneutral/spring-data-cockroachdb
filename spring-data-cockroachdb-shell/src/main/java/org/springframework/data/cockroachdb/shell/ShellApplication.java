package org.springframework.data.cockroachdb.shell;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.annotation.Order;
import org.springframework.data.cockroachdb.shell.support.HybridShellRunner;
import org.springframework.shell.boot.ApplicationRunnerAutoConfiguration;
import org.springframework.shell.jline.InteractiveShellRunner;

@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        ApplicationRunnerAutoConfiguration.class
})
@EnableConfigurationProperties
@ComponentScan(basePackageClasses = ShellApplication.class)
@Order(InteractiveShellRunner.PRECEDENCE - 100)
public class ShellApplication implements ApplicationRunner {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ShellApplication.class)
                .web(WebApplicationType.NONE)
                .headless(false)
                .logStartupInfo(true)
                .run(args);
    }

    @Autowired
    private HybridShellRunner hybridShellRunner;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        hybridShellRunner.run(args);
    }
}

