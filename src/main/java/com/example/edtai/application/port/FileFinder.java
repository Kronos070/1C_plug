package com.example.edtai.application.port;

import java.util.List;

@FunctionalInterface
public interface FileFinder {

    List<String> find(String query, int limit);
}
