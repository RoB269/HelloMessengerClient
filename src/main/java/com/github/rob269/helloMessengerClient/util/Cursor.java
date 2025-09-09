package com.github.rob269.helloMessengerClient.util;


import java.util.Iterator;
import java.util.NoSuchElementException;

public class Cursor<T> implements Iterator<T> {
    private Node<T> node;

    public Cursor(Node<T> node) {
        this.node = new Node<>(null);
        this.node.previous = node;
        this.node.next = node;
    }

    @Override
    public boolean hasNext() {
        return node.next != null;
    }

    public boolean hasPrevious() {
        return node.previous != null;
    }

    @Override
    public T next() {
        if (node.next == null) throw new NoSuchElementException();
        node = node.next;
        return node.val;
    }

    public T previous() {
        if (node.previous == null) throw new NoSuchElementException();
        node = node.previous;
        return node.val;
    }
}
