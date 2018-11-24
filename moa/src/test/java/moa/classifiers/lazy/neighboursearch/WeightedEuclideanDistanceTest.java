package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class WeightedEuclideanDistanceTest {

    private static final double EPSILON = 0.000001;

    private WeightedEuclideanDistance weightedEuclideanDistance;

    // Test parameters
    private final String relation;
    private final List<Attribute> attributes;
    private final double[] firstInstanceData;
    private final double[] secondInstanceData;
    private final double expectedDistance;

    public WeightedEuclideanDistanceTest(String relation, List<Attribute> attributes, double[] firstInstanceData, double[] secondInstanceData, double expectedDistance) {
        this.relation = relation;
        this.attributes = attributes;
        this.firstInstanceData = firstInstanceData;
        this.secondInstanceData = secondInstanceData;
        this.expectedDistance = expectedDistance;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "rel1", createAttributes(4), new double[] {0.5, 0.9, 0.2, 0}, new double[] {0.3, 0.2, 0.8, 1}, Math.sqrt(2.37) },
                { "rel2", createAttributes(3), new double[] {0.9, 0, 1}, new double[] {0.5, 0.8, 0}, Math.sqrt(0.96) }
        });
    }

    @Test
    public void shouldComputeCorrectWeightedDistance() {
        // given
        Instance firstInstance = new DenseInstance(0, firstInstanceData);
        Instance secondInstance = new DenseInstance(0, secondInstanceData);

        Instances instances = new Instances(relation, attributes, 2);
        instances.add(firstInstance);
        instances.add(secondInstance);
        instances.setClassIndex(firstInstanceData.length - 1);

        weightedEuclideanDistance = new WeightedEuclideanDistance(instances, DoubleStream.of(2., 1., 5.).boxed().collect(toList()));
        weightedEuclideanDistance.setDontNormalize(true);

        // when
        double distance = weightedEuclideanDistance.distance(firstInstance, secondInstance);

        // then
        assertEquals(expectedDistance, distance, EPSILON);
    }

    private static List<Attribute> createAttributes(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new Attribute(String.format("Attribute%s", i)))
                .collect(toList());
    }
}
