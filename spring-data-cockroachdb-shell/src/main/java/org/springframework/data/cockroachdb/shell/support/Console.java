package org.springframework.data.cockroachdb.shell.support;

import java.util.Locale;

import org.jline.terminal.Terminal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class Console {
    private final Terminal terminal;

    @Autowired
    public Console(@Lazy Terminal terminal) {
        Assert.notNull(terminal, "terminal is null");
        this.terminal = terminal;
    }

    public void success(String text) {
        other(AnsiColor.BRIGHT_GREEN, text);
    }

    public void success(String format, Object... args) {
        other(AnsiColor.BRIGHT_GREEN, format, args);
    }

    public void information(String text) {
        other(AnsiColor.BRIGHT_BLUE, text);
    }

    public void information(String format, Object... args) {
        other(AnsiColor.BRIGHT_BLUE, format, args);
    }

    public void warning(String text) {
        other(AnsiColor.BRIGHT_YELLOW, text);
    }

    public void warning(String format, Object... args) {
        other(AnsiColor.BRIGHT_YELLOW, format, args);
    }

    public void error(String text) {
        other(AnsiColor.BRIGHT_RED, text);
    }

    public void error(String format, Object... args) {
        other(AnsiColor.BRIGHT_RED, format, args);
    }

    public void other(AnsiColor color, String text) {
        terminal.writer().println(ansiColor(color, text));
        terminal.writer().flush();
    }

    public void other(AnsiColor color, String format, Object... args) {
        terminal.writer().println(ansiColor(color, String.format(Locale.US, format, args)));
        terminal.writer().flush();
    }

    private String ansiColor(AnsiColor color, String message) {
        return AnsiOutput.toString(color, message, AnsiColor.DEFAULT);
    }
}
