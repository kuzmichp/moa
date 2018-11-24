package moa.classifiers.lazy.neighboursearch;

import com.yahoo.labs.samoa.instances.Instances;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class WeightedEuclideanDistance extends EuclideanDistance {

    private final List<Double> weights;
    private ListIterator<Double> iterator;

    public WeightedEuclideanDistance(Instances instances, List<Double> weights) {
        super(instances);
        this.weights = weights;
        iterator = weights.listIterator();
    }

    @Override
    protected double updateDistance(double currDist, double diff) {
        double weight = 0;
        try {
            weight = iterator.next();
        } catch (NoSuchElementException e) {
            iterator = weights.listIterator();
            weight = iterator.next();
        }
        return currDist + weight * diff * diff;
    }
}
