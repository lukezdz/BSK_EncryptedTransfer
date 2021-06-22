package pl.edu.pg.bsk.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ModifiableObservableListBase;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ArrayObservableList<E> extends ModifiableObservableListBase<E> {
	@Getter
	BooleanProperty isEmpty = new SimpleBooleanProperty();

	private final List<E> delegate = new ArrayList<>();

	public E get(int index) {
		return delegate.get(index);
	}

	public int size() {
		return delegate.size();
	}

	protected void doAdd(int index, E element) {
		delegate.add(index, element);
		updateIsEmpty();
	}

	protected E doSet(int index, E element) {
		E value = delegate.set(index, element);
		updateIsEmpty();
		return value;
	}

	protected E doRemove(int index) {
		E value = delegate.remove(index);
		updateIsEmpty();
		return value;
	}

	private void updateIsEmpty() {
		isEmpty.set(delegate.isEmpty());
	}
}
