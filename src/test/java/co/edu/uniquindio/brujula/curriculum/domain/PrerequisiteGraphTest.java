package co.edu.uniquindio.brujula.curriculum.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Prerequisite graph and acyclicity service tests.
 *
 * <p>Covers: direct cycle (2-cycle), multi-hop cycle, self-loop, valid DAG.
 */
class PrerequisiteGraphTest {

  private UUID courseA;
  private UUID courseB;
  private UUID courseC;
  private UUID courseD;
  private PrerequisiteAcyclicityService service;

  @BeforeEach
  void setUp() {
    courseA = UUID.randomUUID();
    courseB = UUID.randomUUID();
    courseC = UUID.randomUUID();
    courseD = UUID.randomUUID();
    service = new PrerequisiteAcyclicityService();
  }

  @Test
  void emptyGraphIsValid() {
    PrerequisiteGraph graph = new PrerequisiteGraph();
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }

  @Test
  void singleEdgeIsValid() {
    PrerequisiteGraph graph = new PrerequisiteGraph();
    // A is prereq for B (edge: A → B meaning B depends on A)
    graph.addEdge(courseA, courseB);
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }

  @Test
  void directTwoNodeCycleRaisesException() {
    // A → B (B depends on A) then B → A (A depends on B) = cycle
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseB, courseA);
    assertThatThrownBy(() -> service.validateAcyclicity(graph))
        .isInstanceOf(PrerequisiteCycleException.class);
  }

  @Test
  void multiHopCycleRaisesException() {
    // A → B → C → A
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseB, courseC);
    graph.addEdge(courseC, courseA);
    assertThatThrownBy(() -> service.validateAcyclicity(graph))
        .isInstanceOf(PrerequisiteCycleException.class);
  }

  @Test
  void selfLoopRaisesException() {
    // A → A
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseA);
    assertThatThrownBy(() -> service.validateAcyclicity(graph))
        .isInstanceOf(PrerequisiteCycleException.class);
  }

  @Test
  void validDagIsAccepted() {
    // A → B, A → C, B → D (diamond shape — valid DAG)
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseA, courseC);
    graph.addEdge(courseB, courseD);
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }

  @Test
  void longerChainWithoutCycleIsValid() {
    // A → B → C → D (linear chain — valid)
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseB, courseC);
    graph.addEdge(courseC, courseD);
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }

  @Test
  void disconnectedNodesAreValid() {
    // A → B and C → D (two disconnected edges — valid)
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseC, courseD);
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }

  @Test
  void fullDiamondDagIsValid() {
    // Full diamond A→B, A→C, B→D, C→D
    // D is reachable from both B and C (two paths from A). The DFS colors D BLACK on first visit
    // and skips it (already BLACK) on second visit — no false PrerequisiteCycleException.
    PrerequisiteGraph graph = new PrerequisiteGraph();
    graph.addEdge(courseA, courseB);
    graph.addEdge(courseA, courseC);
    graph.addEdge(courseB, courseD);
    graph.addEdge(courseC, courseD);
    assertThatCode(() -> service.validateAcyclicity(graph)).doesNotThrowAnyException();
  }
}
