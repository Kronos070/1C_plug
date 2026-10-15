package com.example.edtai.application.port;

import java.io.IOException;
import java.util.Optional;

public interface ReviewFiles {

    Optional<String> read(String path);

    void write(String path, String text) throws IOException;

    void delete(String path) throws IOException;
}
