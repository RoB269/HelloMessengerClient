package com.github.rob269.helloMessengerClient.util;

public class Node<T> {
    public T val;
    public Node<T> next = null;
    public Node<T> previous = null;

    public Node(T val) {
        this.val = val;
    }
}
