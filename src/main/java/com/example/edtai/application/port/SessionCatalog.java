package com.example.edtai.application.port;

import com.example.edtai.domain.SessionSummary;
import java.util.List;

@FunctionalInterface
public interface SessionCatalog {

    List<SessionSummary> list(String currentSessionFile);
}
