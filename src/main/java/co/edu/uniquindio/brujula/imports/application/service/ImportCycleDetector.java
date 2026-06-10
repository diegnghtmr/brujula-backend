package co.edu.uniquindio.brujula.imports.application.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Self-contained directed-graph cycle detector for intra-batch import validation.
 *
 * <p>Used by {@link RequirementsValidator} to detect circular sequences within a REQUIREMENTS
 * import batch — without depending on {@code curriculum.domain.PrerequisiteAcyclicityService} or
 * {@code PrerequisiteGraph} (which would violate the {@code imports} module boundary; imports may
 * only depend on {@code curriculum::api} = {@code curriculum.application.port.in}).
 *
 * <p>Nodes are arbitrary string identifiers (course codes or synthetic IDs). Uses a 3-color DFS
 * (WHITE → GRAY → BLACK), matching the algorithm in {@code curriculum.domain}.
 *
 * <p>Framework-free — no Spring or JPA imports (ARQ-002).
 */
class ImportCycleDetector {

  private final Map<String, List<String>> adjacency = new HashMap<>();

  /**
   * Add a directed edge: {@code from} → {@code to}.
   *
   * <p>Meaning: course {@code to} depends on (requires) course {@code from}.
   *
   * @param from prerequisite course identifier
   * @param to dependent course identifier
   */
  void addEdge(String from, String to) {
    adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    adjacency.putIfAbsent(to, new ArrayList<>());
  }

  /** Returns all registered node identifiers. */
  Set<String> nodes() {
    return Collections.unmodifiableSet(adjacency.keySet());
  }

  /**
   * Returns whether the current graph contains at least one cycle.
   *
   * @return {@code true} if a cycle exists; {@code false} if the graph is acyclic (DAG)
   */
  boolean hasCycle() {
    Map<String, Color> colors = new HashMap<>();
    for (String node : adjacency.keySet()) {
      colors.put(node, Color.WHITE);
    }
    for (String node : adjacency.keySet()) {
      if (colors.get(node) == Color.WHITE) {
        if (dfsVisit(node, colors)) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * DFS visit: marks the node GRAY (in-stack), recurses on successors, marks BLACK when done.
   * Returns {@code true} if a cycle (back-edge to a GRAY node) is detected.
   */
  private boolean dfsVisit(String node, Map<String, Color> colors) {
    colors.put(node, Color.GRAY);
    for (String successor : adjacency.getOrDefault(node, List.of())) {
      Color successorColor = colors.getOrDefault(successor, Color.WHITE);
      if (successorColor == Color.GRAY) {
        return true; // back-edge → cycle
      }
      if (successorColor == Color.WHITE) {
        if (dfsVisit(successor, colors)) {
          return true;
        }
      }
    }
    colors.put(node, Color.BLACK);
    return false;
  }

  private enum Color {
    WHITE, // not yet visited
    GRAY, // currently in the DFS stack (ancestor)
    BLACK // fully processed
  }
}
