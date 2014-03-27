package cz.tul.dic.data;

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
        if (data == null) {
            throw new NullPointerException();
        }

        data.add(item);
    }

    public void addItem(final T item, final int position) {
        while (position > data.size()) {
            data.add(null);
        }
        addItem(item);
    }

    public T getItem(final int position) {
        T result;
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

}