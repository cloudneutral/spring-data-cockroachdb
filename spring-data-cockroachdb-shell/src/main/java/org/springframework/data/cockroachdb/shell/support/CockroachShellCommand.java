package org.springframework.data.cockroachdb.shell.support;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.Availability;
import org.springframework.shell.component.ConfirmationInput;
import org.springframework.shell.component.StringInput;
import org.springframework.shell.standard.AbstractShellComponent;

public abstract class CockroachShellCommand extends AbstractShellComponent {
    private static boolean connected;

    public static void setConnected(boolean connected) {
        CockroachShellCommand.connected = connected;
    }

    public static boolean isConnected() {
        return connected;
    }

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    protected Console console;

    public Availability notConnectedCheck() {
        return isConnected()
                ? Availability.unavailable("you are already connected to a database")
                : Availability.available();
    }

    public Availability connectedCheck() {
        return isConnected()
                ? Availability.available()
                : Availability.unavailable("you are not connected to a database");
    }

    protected void publishEvent(ApplicationEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    protected String askForStringInput(String name, String defaultValue, boolean masked) {
        StringInput component = new StringInput(getTerminal(), name, defaultValue);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        if (masked) {
            component.setMaskCharacter('*');
        }
        StringInput.StringInputContext context = component.run(StringInput.StringInputContext.empty());
        return context.getResultValue();
    }

    protected boolean askForConfirmation(String name, boolean defaultValue) {
        ConfirmationInput component = new ConfirmationInput(getTerminal(), name, defaultValue);
        component.setResourceLoader(getResourceLoader());
        component.setTemplateExecutor(getTemplateExecutor());
        ConfirmationInput.ConfirmationInputContext context = component.run(
                ConfirmationInput.ConfirmationInputContext.empty());
        return context.getResultValue();
    }
}
