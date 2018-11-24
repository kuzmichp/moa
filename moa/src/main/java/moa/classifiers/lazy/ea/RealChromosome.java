package moa.classifiers.lazy.ea;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import moa.classifiers.lazy.InstanceProvider;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.classifiers.lazy.neighboursearch.WeightedEuclideanDistance;
import org.apache.commons.math3.exception.util.DummyLocalizable;
import org.apache.commons.math3.genetics.AbstractListChromosome;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.InvalidRepresentationException;
import org.apache.commons.math3.genetics.Population;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RealChromosome extends AbstractListChromosome<Double> {

    private static final Logger logger = Logger.getLogger(RealChromosome.class.getName());

    private InstanceProvider instanceProvider;
    private FitnessCache<Chromosome, Double> cache;
    private Population population;
    private NearestNeighbourSearch neighbourSearch;
    private int k;

    public RealChromosome(List<Double> representation,
                   InstanceProvider instanceProvider,
                   FitnessCache<Chromosome, Double> cache, Population population,
                   NearestNeighbourSearch neighbourSearch,
                   int k) {
        super(representation);
        this.instanceProvider = instanceProvider;
        this.cache = cache;
        this.population = population;
        this.neighbourSearch = neighbourSearch;
        this.k = k;
    }

    @Override
    public List<Double> getRepresentation() {
        return super.getRepresentation();
    }

    @Override
    public AbstractListChromosome<Double> newFixedLengthChromosome(List<Double> list) {
        return new RealChromosome(list, instanceProvider, cache, population, neighbourSearch, k);
    }

    @Override
    protected void checkValidity(List<Double> genes) {
        if (genes.stream().anyMatch(gene -> gene < 0 || gene > 1)) {
            throw new InvalidRepresentationException(new DummyLocalizable("Genes should be in the [0; 1] range"));
        }
    }

    @Override
    public double fitness() {
        Optional<Double> cachedFitness = cache.get(this);
        if (cachedFitness.isPresent()) {
            return cachedFitness.get();
        }
        double fitness = calculate();
        cache.put(this, fitness);
        return fitness;
    }

    private double calculate() {
        double correctlyPredicted = 0;
        Instances trainingInstances = instanceProvider.getTrainingInstances();
        Instances testInstances = instanceProvider.getTestInstances();
        try {
            neighbourSearch.setInstances(trainingInstances);
            neighbourSearch.setDistanceFunction(new WeightedEuclideanDistance(trainingInstances, getRepresentation()));
            for (int i = 0; i < testInstances.size(); i++) {
                Instance instance = testInstances.get(i);
                correctlyPredicted += getPrediction(instance, trainingInstances) == instance.classValue() ? 1 : 0;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot calculate chromosome's fitness", e);
            return 0;
        }
        return correctlyPredicted / testInstances.size();
    }

    private double getPrediction(Instance instance, Instances trainingInstances) throws Exception {
        Instances neighbours = neighbourSearch.kNearestNeighbours(instance, Math.min(k, trainingInstances.size()));
        Map<Double, Long> votes = IntStream.range(0, neighbours.size())
                .mapToObj(neighbours::get)
                .collect(Collectors.groupingBy(Instance::classValue, Collectors.counting()));
        return Collections.max(votes.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    @Override
    protected boolean isSame(Chromosome another) {
        return equals(another);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Double gene : getRepresentation()) {
            stringBuilder
                    .append(BigDecimal.valueOf(gene)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue())
                    .append(" ");
        }
        return String.format("(f=%s '%s')", getFitness(), stringBuilder.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RealChromosome that = (RealChromosome) o;
        return Objects.equals(getRepresentation(), that.getRepresentation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRepresentation());
    }
}