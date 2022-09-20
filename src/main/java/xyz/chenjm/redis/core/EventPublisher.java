package xyz.chenjm.redis.core;

import java.util.ArrayList;
import java.util.List;

public class EventPublisher<T extends Event> implements EventListener<T>{
    List<EventListener<T>> listeners = new ArrayList<>();
    public void addListener(EventListener<T> e) {
        listeners.add(e);
    }
    @Override
    public void onEvent(T e) {
        listeners.forEach(listener -> listener.onEvent(e));
    }
}
