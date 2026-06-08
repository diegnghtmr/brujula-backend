package co.edu.uniquindio.brujula.imports.domain.model;

import java.util.Map;
import java.util.Objects;

/**
 * Value object: a single parsed data row from an import file.
 *
 * <p>{@code index} is the 0-based position in the original file (1-based in error reporting).
 * {@code cells} maps column header names to raw string values.
 *
 * <p>Pure domain record — zero framework annotations (ARQ-002).
 */
public record ImportRow(int index, Map<String, String> cells) {

    public ImportRow {
        Objects.requireNonNull(cells, "cells must not be null");
        if (index < 0) {
            throw new IllegalArgumentException("index must be >= 0");
        }
        cells = Map.copyOf(cells); // immutable snapshot
    }
}
