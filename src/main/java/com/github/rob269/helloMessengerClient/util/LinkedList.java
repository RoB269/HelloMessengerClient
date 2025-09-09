package com.github.rob269.helloMessengerClient.util;

import java.util.NoSuchElementException;

public class LinkedList<T> {
    private int size = 0;
    private Node<T> firstNode = null;
    private Node<T> lastNode = null;

    public void add(T val) {
        addLast(val);
    }

    public void addLast(T val) {
        Node<T> newNode = new Node<>(val);
        if (lastNode != null) {
            lastNode.next = newNode;
            if (size == 1) {
                firstNode = lastNode;
                newNode.previous = firstNode;
            }
            else {
                newNode.previous = lastNode;
            }
        }
        lastNode = newNode;
        size++;
    }

    public void addFirst(T val) {
        if (lastNode == null) {
            addLast(val);
            return;
        }
        if (firstNode == null) {
            firstNode = new Node<>(val);
            firstNode.next = lastNode;
            lastNode.previous = firstNode;
            size++;
            return;
        }
        Node<T> newNode = new Node<>(val);
        newNode.next = firstNode;
        firstNode.previous = newNode;
        firstNode = newNode;
        size++;
    }

    public T getLast() {
        if (size == 0) throw new NoSuchElementException();
        return lastNode.val;
    }

    public T getFirst() {
        if (firstNode == null) return lastNode.val;
        return firstNode.val;
    }

    public int size() {
        return size;
    }

    public Cursor<T> getLastCursor() {
        return new Cursor<>(lastNode);
    }

    public Cursor<T> getFirstCursor() {
        if (firstNode == null) return new Cursor<>(lastNode);
        return new Cursor<>(firstNode);
    }

    public boolean isEmpty() {
        return size == 0;
    }
}
