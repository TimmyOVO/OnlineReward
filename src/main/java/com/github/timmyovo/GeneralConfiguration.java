package com.github.timmyovo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class GeneralConfiguration {
    private Map<Integer, List<String>> commandsMap;
}
