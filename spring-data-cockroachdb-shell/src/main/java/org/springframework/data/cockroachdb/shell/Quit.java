package org.springframework.data.cockroachdb.shell;

import org.springframework.data.cockroachdb.shell.support.CockroachFacts;
import org.springframework.data.cockroachdb.shell.support.CockroachShellCommand;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
@ShellCommandGroup(value = "Built-In Commands")
public class Quit extends CockroachShellCommand implements org.springframework.shell.standard.commands.Quit.Command {
    @ShellMethod(value = "Exit the shell", key = {"quit", "exit", "q"})
    public void quit() {
        console.success("Did you know? %s", CockroachFacts.nextFact());
        throw new ExitRequest();
    }
}
