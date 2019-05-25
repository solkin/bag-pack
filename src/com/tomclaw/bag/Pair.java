package com.tomclaw.bag;

/**
 * Created by solkin on 6/12/16.
 */
@SuppressWarnings("WeakerAccess")
public class Pair<Key, Value> {

    private Key key;
    private Value value;

    public Pair(Key key, Value value) {
        this.key = key;
        this.value = value;
    }

    public Key getKey() {
        return key;
    }

    public Value getValue() {
        return value;
    }
}
