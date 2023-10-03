package org.springframework.data.cockroachdb.shell.support;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.utils.AttributedString;
import org.springframework.boot.ApplicationArguments;
import org.springframework.shell.ExitRequest;
import org.springframework.shell.Input;
import org.springframework.shell.InputProvider;
import org.springframework.shell.Shell;
import org.springframework.shell.ShellRunner;
import org.springframework.shell.Utils;
import org.springframework.shell.context.InteractionMode;
import org.springframework.shell.context.ShellContext;
import org.springframework.shell.jline.PromptProvider;

public class HybridShellRunner implements ShellRunner {
    final LineReader lineReader;

    final PromptProvider promptProvider;

    final Shell shell;

    final ShellContext shellContext;

    final Function<ApplicationArguments, List<String>> commandsFromInputArgs = args -> args
            .getSourceArgs().length == 0 ? Collections.emptyList()
            : Collections.singletonList(String.join(" ", args.getSourceArgs()));

    public HybridShellRunner(LineReader lineReader,
                             PromptProvider promptProvider,
                             Shell shell,
                             ShellContext shellContext) {
        this.lineReader = lineReader;
        this.promptProvider = promptProvider;
        this.shell = shell;
        this.shellContext = shellContext;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        InputProvider inputProvider = new JLineInputProvider(lineReader, promptProvider);

        if (!commandsFromInputArgs.apply(args).isEmpty()) {
            Parser lineParser = new DefaultParser();
            List<String> commands = this.commandsFromInputArgs.apply(args);
            List<ParsedLine> parsedLines = commands.stream()
                    .map(rawCommandLine -> lineParser.parse(rawCommandLine, rawCommandLine.length() + 1))
                    .collect(Collectors.toList());

            inputProvider = new MultiParsedLineInputProvider(parsedLines, inputProvider);
        }

        shellContext.setInteractionMode(InteractionMode.INTERACTIVE);

        shell.run(inputProvider);
    }

    @Override
    public boolean canRun(ApplicationArguments args) {
        return true;
    }

    public static class JLineInputProvider implements InputProvider {
        final LineReader lineReader;

        final PromptProvider promptProvider;

        public JLineInputProvider(LineReader lineReader, PromptProvider promptProvider) {
            this.lineReader = lineReader;
            this.promptProvider = promptProvider;
        }

        @Override
        public Input readInput() {
            try {
                AttributedString prompt = promptProvider.getPrompt();
                lineReader.readLine(prompt.toAnsi(lineReader.getTerminal()));
            } catch (UserInterruptException e) {
                if (e.getPartialLine().isEmpty()) {
                    throw new ExitRequest(1);
                } else {
                    return Input.EMPTY;
                }
            } catch (EndOfFileException e) {
                throw new ExitRequest(1);
            }
            return new ParsedLineInput(lineReader.getParsedLine());
        }
    }

    static class MultiParsedLineInputProvider implements InputProvider {
        final List<ParsedLineInput> parsedLineInputs;

        final InputProvider fallbackProvider;

        int inputIdx;

        MultiParsedLineInputProvider(List<ParsedLine> parsedLines, InputProvider fallbackProvider) {
            this.parsedLineInputs = parsedLines.stream()
                    .map(ParsedLineInput::new)
                    .collect(Collectors.toList());
            this.fallbackProvider = fallbackProvider;
        }

        @Override
        public Input readInput() {
            if (inputIdx == parsedLineInputs.size()) {
                return fallbackProvider.readInput();
            }
            return parsedLineInputs.get(inputIdx++);
        }
    }

    static class ParsedLineInput implements Input {
        final ParsedLine parsedLine;

        ParsedLineInput(ParsedLine parsedLine) {
            this.parsedLine = parsedLine;
        }

        @Override
        public String rawText() {
            return parsedLine.line();
        }

        @Override
        public List<String> words() {
            return Utils.sanitizeInput(parsedLine.words());
        }
    }
}
