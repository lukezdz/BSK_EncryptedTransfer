package pl.edu.pg.bsk.utils;

import javafx.collections.ModifiableObservableListBase;

import java.util.ArrayList;
import java.util.List;

public class ArrayObservableList<E> extends ModifiableObservableListBase<E> {

	private final List<E> delegate = new ArrayList<>();

	public E get(int index) {
		return delegate.get(index);
	}

	public int size() {
		return delegate.size();
	}

	protected void doAdd(int index, E element) {
		delegate.add(index, element);
	}

	protected E doSet(int index, E element) {
		return delegate.set(index, element);
	}

	protected E doRemove(int index) {
		return delegate.remove(index);
	}
}
