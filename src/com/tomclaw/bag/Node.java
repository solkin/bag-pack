package com.tomclaw.bag;

import java.util.Collection;
import java.util.HashMap;

/**
 * Created by solkin on 15/05/16.
 */
public class Node extends HashMap<String, Node> {

    private Node parent;
    private String name;
    private long length;

    public Node(String name, long length) {
        this.name = name;
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public Node getParent() {
        return parent;
    }

    public void setParent(Node parent) {
        this.parent = parent;
    }

    public void add(String path, long length) {
        String[] items = path.split("/");
        Node node = this;
        for (int c = 1; c < items.length; c++) {
            long itemLength = -1;
            if (c == items.length - 1) {
                itemLength = length;
            }
            node = node.get(items[c], itemLength);
        }
    }

    public void add(Node node) {
        node.setParent(this);
        put(node.getName(), node);
    }

    public Node get(String name, long length) {
        Node node = super.get(name);
        if (node == null) {
            node = new Node(name, length);
            add (node);
        }
        return node;
    }

    public Collection<Node> list() {
        return values();
    }
}
