package co.edu.uniquindio.brujula.curriculum.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory directed graph of course prerequisite relationships.
 *
 * <p>Nodes are course UUIDs. A directed edge {@code from → to} means course {@code to} depends on
 * (requires) course {@code from}. The edge direction follows the requirement relationship: if
 * course B requires course A, the edge is {@code A → B}.
 *
 * <p>This graph is used by {@link PrerequisiteAcyclicityService} to enforce the DAG invariant
 * (CUR-003, ADR-F2-006). It is rebuilt in-memory per validation; it has NO Spring/JPA imports
 * (ARQ-002).
 */
public class PrerequisiteGraph {

  /** Adjacency list: node → list of successors (courses that depend on this course). */
  private final Map<UUID, List<UUID>> adjacency = new HashMap<>();

  /**
   * Add a directed edge from {@code from} to {@code to}.
   *
   * <p>Meaning: course {@code to} depends on (requires) course {@code from}. Both nodes are
   * registered in the graph even if they have no other edges.
   *
   * @param from the prerequisite course (must have been approved first)
   * @param to the course that requires {@code from}
   */
  public void addEdge(UUID from, UUID to) {
    adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
    // Ensure to-node is registered even if it has no outgoing edges
    adjacency.putIfAbsent(to, new ArrayList<>());
  }

  /** Returns all nodes (course UUIDs) in the graph. */
  public Set<UUID> nodes() {
    return Collections.unmodifiableSet(adjacency.keySet());
  }

  /**
   * Returns the direct successors (dependents) of a given node. Returns an empty list if the node
   * has no outgoing edges.
   */
  public List<UUID> successors(UUID node) {
    return Collections.unmodifiableList(adjacency.getOrDefault(node, List.of()));
  }
}
