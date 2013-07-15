package net.arnx.xmlic;

public interface Translator<T> {
	public T translate(int i, T value);
}
