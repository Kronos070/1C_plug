package com.example.edtai.domain;

public record DocCaller(String id, String name, String path, int line) {

    public DocCaller {
        id = id == null ? "" : id;
        name = name == null ? "" : name;
        path = path == null ? "" : path;
    }
}
