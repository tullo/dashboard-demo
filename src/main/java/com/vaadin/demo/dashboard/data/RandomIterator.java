package com.vaadin.demo.dashboard.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RandomIterator<T> implements Iterator<T> {

    private final Iterator<T> iterator;

    public RandomIterator(Set<T> countries) {
        final List<T> items = new ArrayList<>();
        Iterator<T> i = countries.iterator();
        while (i.hasNext()) {
            items.add(i.next());
        }
        Collections.shuffle(items);
        iterator = items.iterator();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public T next() {
        return iterator.next();
    }
}
