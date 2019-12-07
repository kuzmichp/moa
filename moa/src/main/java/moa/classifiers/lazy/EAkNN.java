package moa.classifiers.lazy;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.ElitisticListPopulation;
import org.apache.commons.math3.genetics.FixedGenerationCount;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.genetics.Population;
import org.apache.commons.math3.genetics.TournamentSelection;
import org.apache.commons.math3.genetics.UniformCrossover;
import org.apache.commons.math3.random.RandomGenerator;

import com.github.javacliparser.FloatOption;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.lazy.ea.FitnessCache;
import moa.classifiers.lazy.ea.PerturbationMutation;
import moa.classifiers.lazy.ea.RealChromosome;
import moa.classifiers.lazy.neighboursearch.LinearNNSearch;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import moa.classifiers.lazy.neighboursearch.WeightedEuclideanDistance;
import moa.core.Measurement;

public class EAkNN extends AbstractClassifier implements MultiClassClassifier {

    private static final long serialVersionUID = 5643496816194975072L;

    private static final Logger logger = Logger.getLogger(EAkNN.class.getName());

    public static final IntOption kOption = new IntOption("k", 'k', "The number of neighbors", 5, 1, Integer.MAX_VALUE);
    public static final IntOption limitOption = new IntOption("limit", 'w', "The maximum number of instances to store", 1000, 1, Integer.MAX_VALUE);

    // EA parameters
    public static final IntOption pOption = new IntOption("p", 'p', "The population size", 30, 10, Integer.MAX_VALUE);
    public static final IntOption eOption = new IntOption("e", 'e', "The number of epochs", 5, 1, Integer.MAX_VALUE);
    public static final IntOption aOption = new IntOption("a", 'a', "The arity", 2, 2, Integer.MAX_VALUE);
    public static final FloatOption cOption = new FloatOption("c", 'c', "The crossover rate", 0.6, 0.0, Float.MAX_VALUE);
    public static final FloatOption mOption = new FloatOption("m", 'm', "The mutation rate", 0.05, 0.0, Float.MAX_VALUE);
    public static final FloatOption lOption = new FloatOption("l", 'l', "The elitism rate", 0.02, 0.0, Float.MAX_VALUE);

    private int c = 0;

    private transient InstanceProvider instanceProvider;
    private transient GeneticAlgorithm geneticAlgorithm;
    private transient Population population;
    private transient FitnessCache<Chromosome, Double> cache;

    @Override
    public void setModelContext(InstancesHeader context) {
        super.setModelContext(context);
        instanceProvider = new InstanceProvider(context, limitOption.getValue());
        cache = new FitnessCache<>();
        int chromosomeLength = context.numAttributes() - 1;
        geneticAlgorithm = new GeneticAlgorithm(new UniformCrossover(0.5), cOption.getValue(),
                new PerturbationMutation(), mOption.getValue(),
                new TournamentSelection(aOption.getValue()));
        // Initialize population
        population = new ElitisticListPopulation(pOption.getValue(), lOption.getValue());
        for (int i = 0; i < pOption.getValue() - 1; i++) {
            Chromosome chromosome = createChromosome(chromosomeLength);
            population.addChromosome(chromosome);
        }
        population.addChromosome(createUnitaryChromosome(chromosomeLength));
    }

    @Override
    public void resetLearningImpl() {
        instanceProvider = null;
        population = null;
        geneticAlgorithm = null;
        cache = null;
    }

    @Override
    public void trainOnInstanceImpl(Instance inst) {
        if (inst.classValue() > c) {
            c = (int) inst.classValue();
        }
        instanceProvider.add(inst);
        if (instanceProvider.isEvolvementNeeded()) {
            population = geneticAlgorithm.evolve(population, new FixedGenerationCount(eOption.getValue()));
            cache.invalidate();
        }
    }

    @Override
    public double[] getVotesForInstance(Instance inst) {
        double[] votes = new double[c + 1];
        try {
            Instances instances = instanceProvider.getInstances();
            RealChromosome fittestChromosome = (RealChromosome) population.getFittestChromosome();
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(Paths.get("solution.txt"), CREATE, APPEND)) {
                bufferedWriter.write(fittestChromosome.toString());
            }
            NearestNeighbourSearch search = new LinearNNSearch(instances);
            search.setDistanceFunction(new WeightedEuclideanDistance(instances, fittestChromosome.getRepresentation()));
            if (instances.size() > 0) {
                int k = Math.min(kOption.getValue(), instances.size());
                Instances neighbours = search.kNearestNeighbours(inst, k);
                for (int i = 0; i < k; i++) {
                    votes[(int) neighbours.get(i).classValue()] += 1;
                }
            }
        } catch (Exception e) {
            String message = String.format("Unable to make prediction of instance %s", inst);
            logger.log(Level.WARNING, message);
            return new double[inst.numClasses()];
        }
        return votes;
    }

    @Override
    public boolean isRandomizable() {
        return false;
    }

    @Override
    protected Measurement[] getModelMeasurementsImpl() {
        return new Measurement[0];
    }

    @Override
    @SuppressWarnings("squid:S1186")
    public void getModelDescription(StringBuilder out, int indent) {

    }

    private Chromosome createChromosome(int length) {
        RandomGenerator generator = GeneticAlgorithm.getRandomGenerator();
        List<Double> genes = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            genes.add(generator.nextDouble());
        }
        return new RealChromosome(genes, instanceProvider, cache, population, new LinearNNSearch(), kOption.getValue());
    }

    private Chromosome createUnitaryChromosome(int length) {
        List<Double> genes = new ArrayList<>(Collections.nCopies(length, 1.0));
        return new RealChromosome(genes, instanceProvider, cache, population, new LinearNNSearch(), kOption.getValue());
    }

    @Override
    public ImmutableCapabilities defineImmutableCapabilities() {
        if (this.getClass() == EAkNN.class)
            return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
        else
            return new ImmutableCapabilities(Capability.VIEW_STANDARD);
    }
}
