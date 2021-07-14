package nextstep.subway.path.domain;

import nextstep.subway.exception.NotValidatePathException;
import nextstep.subway.line.domain.Line;
import nextstep.subway.line.domain.Section;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.domain.Station;
import nextstep.subway.station.domain.StationsResponse;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.WeightedMultigraph;

import java.util.List;

import static nextstep.subway.path.domain.Fare.DEFAULT_FARE;
import static nextstep.subway.path.domain.Fare.MINIMUM_FARE;

public class PathFinder {

    private final GraphPath<Station, DefaultWeightedEdge> path;
    private final Fare fare;

    public PathFinder(Station source, Station target, List<Line> lines) {
        validateEquals(source, target);

        fare = new Fare(DEFAULT_FARE + maxSurcharge(lines));
        path = findShortest(source, target, lines);
    }

    private void validateEquals(Station source, Station target) {
        if (source.equals(target)) {
            throw new NotValidatePathException();
        }
    }

    private GraphPath<Station, DefaultWeightedEdge> findShortest(Station source, Station target, List<Line> lines) {
        WeightedMultigraph<Station, DefaultWeightedEdge> graph = new WeightedMultigraph<>(DefaultWeightedEdge.class);

        addAllLineSectionsToGraph(lines, graph);

        return createShortestPaths(source, target, graph);
    }

    private void addAllLineSectionsToGraph(List<Line> lines, WeightedMultigraph<Station, DefaultWeightedEdge> graph) {
        lines.stream()
                .map(Line::getSections)
                .forEach(sections -> addSectionsToGraph(graph, sections));
    }

    private GraphPath<Station, DefaultWeightedEdge> createShortestPaths(
            Station source,
            Station target,
            WeightedMultigraph<Station, DefaultWeightedEdge> graph
    ) {
        GraphPath<Station, DefaultWeightedEdge> path = new DijkstraShortestPath<>(graph).getPath(source, target);

        if (path == null) {
            throw new NotValidatePathException();
        }

        return path;
    }

    private Integer maxSurcharge(List<Line> lines) {
        return lines.stream()
                .map(Line::getSurcharge)
                .max(Integer::compare)
                .orElse(MINIMUM_FARE);
    }

    public PathResponse findShortestPathToResponse(int age) {
        List<Station> shortestStations = findShortestPath();

        int distance = calculateShortestDistance();

        int calculateFare = fare.calculateFare(age, distance);

        return new PathResponse(StationsResponse.of(shortestStations), distance, calculateFare);
    }

    private List<Station> findShortestPath() {
        return path.getVertexList();
    }

    private int calculateShortestDistance() {
        return (int) path.getWeight();
    }

    private void addSectionsToGraph(WeightedMultigraph<Station, DefaultWeightedEdge> graph, List<Section> sections) {
        for (Section section : sections) {
            addSectionToGraph(graph, section);
        }
    }

    private void addSectionToGraph(WeightedMultigraph<Station, DefaultWeightedEdge> graph, Section section) {
        Station upStation = section.getUpStation();
        Station downStation = section.getDownStation();

        graph.addVertex(upStation);
        graph.addVertex(downStation);
        graph.setEdgeWeight(graph.addEdge(upStation, downStation), section.getDistance());
    }

    public Fare getFare() {
        return fare;
    }
}
