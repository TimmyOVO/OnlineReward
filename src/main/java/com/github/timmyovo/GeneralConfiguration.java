package com.github.timmyovo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class GeneralConfiguration {
    private Map<Integer, String> commandsMap;
}
