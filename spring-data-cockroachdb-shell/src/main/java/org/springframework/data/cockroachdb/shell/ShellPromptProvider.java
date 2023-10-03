package org.springframework.data.cockroachdb.shell;

import java.sql.SQLException;

import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.context.event.EventListener;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import org.springframework.data.cockroachdb.shell.event.CommandExceptionEvent;
import org.springframework.data.cockroachdb.shell.event.ConnectedEvent;
import org.springframework.data.cockroachdb.shell.event.DisconnectedEvent;
import org.springframework.data.cockroachdb.shell.event.TransactionEndedEvent;
import org.springframework.data.cockroachdb.shell.event.TransactionStartedEvent;

@Component
public class ShellPromptProvider implements PromptProvider {
    private enum TransactionState {
        OPEN {
            @Override
            AttributedStyle foregroundColor() {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW);
            }
        },
        ERROR {
            @Override
            AttributedStyle foregroundColor() {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
                        .background(AttributedStyle.YELLOW);
            }
        },
        NONE {
            @Override
            AttributedStyle foregroundColor() {
                return AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN);
            }
        };

        abstract AttributedStyle foregroundColor();
    }

    private TransactionState transactionState = TransactionState.NONE;

    private String connectionPrompt;

    @EventListener
    public void handle(ConnectedEvent event) {
        this.connectionPrompt = event.getConnectionPrompt();
    }

    @EventListener
    public void handle(DisconnectedEvent event) {
        this.connectionPrompt = null;
    }

    @EventListener
    public void handle(TransactionStartedEvent event) {
        this.transactionState = TransactionState.OPEN;
    }

    @EventListener
    public void handle(TransactionEndedEvent event) {
        this.transactionState = TransactionState.NONE;
    }

    @EventListener
    public void handle(CommandExceptionEvent event) {
        if (transactionState.equals(TransactionState.OPEN)) {
            Throwable cause = NestedExceptionUtils.getMostSpecificCause(event.getThrowable());
            if (cause instanceof SQLException) {
                this.transactionState = TransactionState.ERROR;
            }
        }
    }

    @Override
    public AttributedString getPrompt() {
        if (connectionPrompt != null) {
            if (transactionState != TransactionState.NONE) {
                return new AttributedString(connectionPrompt + " " + transactionState.name() + "> ",
                        transactionState.foregroundColor());
            }
            return new AttributedString(connectionPrompt + "> ",
                    AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
        }
        return new AttributedString("(disconnected)> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.WHITE));
    }
}
