package com.github.timmyovo;

public class ImpossibleException extends RuntimeException {
    public ImpossibleException() {
        super("不存在的兄弟");
    }
}
