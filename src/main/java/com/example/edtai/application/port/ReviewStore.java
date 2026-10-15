package com.example.edtai.application.port;

import com.example.edtai.domain.ReviewEntry;
import java.util.List;
import java.util.Optional;

public interface ReviewStore {

    Optional<ReviewEntry> find(String path);

    List<ReviewEntry> all();

    void save(ReviewEntry entry);

    void remove(String path);
}
