package com.example.edtai.application.port;

public interface CodeReview {

    void writing(String path, String tool);

    void written(String path, String tool, String patch);
}
