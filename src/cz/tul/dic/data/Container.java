/* Copyright (C) LENAM, s.r.o. - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 * Written by Petr Jecmen <petr.jecmen@tul.cz>, 2015
 */
package cz.tul.dic.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public final class Container<T extends Serializable> implements Serializable {

    private final List<T> data;

    public Container() {
        data = new ArrayList<>();
    }

    public Container(Container<T> container) {
        data = new LinkedList<>(container.data);
    }

    public void setItem(final T item, final int position) {
        while (position >= data.size()) {
            data.add(null);
        }

        data.set(position, item);
    }

    public T getItem(final int position) {
        T result;
        if (position >= data.size()) {
            if (!data.isEmpty()) {
                result = getItem(data.size() - 1);
            } else {
                result = null;
            }
        } else if (position < 0) {
            return null;
        } else {
            result = data.get(position);
            if (result == null) {
                result = getItem(position - 1);
            }
        }
        return result;
    }

    public T getItemPrecise(final int position) {
        if (position < 0 || position >= data.size()) {
            return null;
        } else {
            return data.get(position);
        }
    }

    public void clear() {
        data.clear();
    }

}
