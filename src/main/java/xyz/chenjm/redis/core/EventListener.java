package xyz.chenjm.redis.core;

public interface EventListener<T extends Event> {
    void onEvent(T e);
}
