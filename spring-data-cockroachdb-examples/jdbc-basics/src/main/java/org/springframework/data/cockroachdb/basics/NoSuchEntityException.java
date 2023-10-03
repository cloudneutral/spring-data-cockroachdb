package org.springframework.data.cockroachdb.basics;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.lang.Nullable;

public class NoSuchEntityException extends DataRetrievalFailureException {
    public NoSuchEntityException(String msg) {
        super(msg);
    }

    public NoSuchEntityException(Class<?> persistentClass, Object identifier) {
        super("Object of class [" + persistentClass.getName() + "] with identifier [" + identifier + "]: not found");
    }

    public NoSuchEntityException(Class<?> persistentClass, Object identifier, @Nullable Throwable cause) {
        super("Object of class [" + persistentClass.getName() + "] with identifier [" + identifier + "]: not found",
                cause);
    }
}
