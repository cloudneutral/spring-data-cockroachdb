package org.springframework.data.cockroachdb.it.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.data.cockroachdb.it.bank.model.Money;

public abstract class RandomUtils {
    private RandomUtils() {
    }

    public static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static Money randomMoneyBetween(double low, double high, String currency) {
        return randomMoneyBetween(low, high, Currency.getInstance(currency));
    }

    public static Money randomMoneyBetween(double low, double high, Currency currency) {
        return Money.of(String.format(Locale.US, "%.2f", random.nextDouble(low, high)), currency);
    }

    public static <E> E selectRandomUnique(Collection<E> collection, Collection<E> observed) {
        List<E> givenList = new ArrayList<>(collection);
        E e;
        do {
            e = givenList.get(random.nextInt(givenList.size()));
        } while (observed.contains(e));
        observed.add(e);
        return e;
    }
}
