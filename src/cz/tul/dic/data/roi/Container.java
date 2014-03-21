package cz.tul.dic.data.roi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Petr Jecmen
 * @param <T>
 */
public final class Container<T> implements Serializable {

    private final List<T> data;

    public Container() {
        data = new ArrayList<>();
    }

    public void addItem(final T item) {
        data.add(item);
    }

    public void addItem(final T item, final int position) {
        while (position >= data.size()) {
            data.add(null);
        }
        data.set(position, item);
    }

    public T getItem(final int position) {
        final T result;
        if (position >= data.size()) {
            if (!data.isEmpty()) {
                result = data.get(data.size() - 1);
            } else {
                result = null;
            }
        } else if (position < 0) {
            return null;
        } else {
            result = data.get(position);
        }
        return result;
    }

}
