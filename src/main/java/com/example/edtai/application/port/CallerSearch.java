package com.example.edtai.application.port;

import com.example.edtai.domain.DocCaller;
import java.util.List;

public interface CallerSearch {

    List<DocCaller> callers(String path, String name, boolean exported);
}
