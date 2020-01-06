package moa.classifiers.lazy;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.EvolutionResult.toUniquePopulation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import com.yahoo.labs.samoa.instances.Instance;

import io.jenetics.Chromosome;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;
import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.lazy.DataProvider.LearningData;
import moa.core.Measurement;
import smile.classification.KNN;
import smile.math.distance.EuclideanDistance;
import smile.math.distance.HammingDistance;

public class HybridEvolutionaryAlgorithm extends AbstractClassifier implements MultiClassClassifier,
		CapabilitiesHandler {

	private static final int LIMIT = 1000;
	private static final double TRAINING_TO_ALL_RATIO = 0.7;
	private static final int POPULATION_SIZE = 30;
	private static final int MAX_EPOCHS = 100;
	private static final int STEADINESS = 10;
	private static final int K = 30;
	private static int I = 1;
	private int c;

	private transient DataProvider dataProvider;
	private transient Genotype<DoubleGene> genotype;

	@Override
	public double[] getVotesForInstance(com.yahoo.labs.samoa.instances.Instance inst) {
		LearningData learningData = dataProvider.getLearningData();
		if (dataProvider.modelIsOutdated() || genotype == null) {
			createOrUpdateModel(inst, learningData);
		}
		return predict(inst, learningData, genotype);
	}

	private void createOrUpdateModel(Instance inst, LearningData learningData) {
		Factory<Genotype<DoubleGene>> genotypeFactory = null;
		Engine.Builder<DoubleGene, Double> builder = null;
		if (genotype == null) {
			genotypeFactory = Genotype.of(DoubleChromosome.of(0, 1, inst.numInputAttributes()));
			builder = Engine.builder(g -> eval(g, learningData), genotypeFactory);
		} else {
			Chromosome<DoubleGene> chromosome = genotype.getChromosome();
			builder = Engine.builder(g -> eval(g, learningData), chromosome);
		}
		EvolutionStatistics<Double, DoubleMomentStatistics> statistics = EvolutionStatistics.ofNumber();
		Engine<DoubleGene, Double> engine = builder.mapping(toUniquePopulation())
				.populationSize(POPULATION_SIZE)
				.build();
		genotype = engine.stream()
				.limit(Limits.bySteadyFitness(STEADINESS))
				.limit(MAX_EPOCHS)
				.peek(statistics)
				.peek(this::monitor)
				.collect(toBestGenotype());
		System.out.println(statistics.toString());
		dataProvider.reset();
		I++;
	}

	private void monitor(EvolutionResult<DoubleGene, Double> evolutionResult) {
		double[] weights = evolutionResult.getBestPhenotype()
				.getGenotype()
				.getChromosome()
				.stream()
				.mapToDouble(DoubleGene::doubleValue).toArray();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("vavel-ea-%d.csv", I), true))) {
			for (double weight : weights) {
				writer.append(String.format("%.2f,", weight));
			}
			writer.newLine();
			//writer.append(String.format("%f,%f%n",weights[0], weights[1]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double eval(Genotype<DoubleGene> g, LearningData learningData) {
		double[] weights = g.getChromosome().stream().mapToDouble(DoubleGene::doubleValue).toArray();
		try {
			KNN<double[]> knn = KNN.fit(learningData.getTrainingAttributes(), learningData.getTrainingClasses(),
					new EuclideanDistance(weights), K);
			return computeFitness(learningData.getTestClasses(), knn.predict(learningData.getTestAttributes()));
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			return 0;
		}
	}

	private static double computeFitness(int[] expected, int[] predicted) {
		int hits = expected.length - HammingDistance.d(expected, predicted);
		return (double) hits / expected.length;
	}

	private double[] predict(com.yahoo.labs.samoa.instances.Instance inst, LearningData data, Genotype<DoubleGene> genotype) {
		double[] weights = genotype.getChromosome().stream()
				.mapToDouble(DoubleGene::doubleValue)
				.toArray();
		double[] votes = new double[c + 1];
		try {
			KNN<double[]> knn = KNN.fit(data.getAllAttributes(), data.getAllClasses(), new EuclideanDistance(weights), K);
			int prediction = knn.predict(Arrays.copyOf(inst.toDoubleArray(), inst.numInputAttributes()));
			votes[prediction] = 1;
			return votes;
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			return votes;
		}
	}

	private void consumeOneGenerationResult(EvolutionResult<DoubleGene, Double> result) {
		ISeq<Phenotype<DoubleGene, Double>> population = result.getPopulation();
		double[][] generation = population.stream()
				.map(Phenotype::getGenotype)
				.map(Genotype::getChromosome)
				.map(ch -> ch.stream().mapToDouble(DoubleGene::doubleValue).toArray())
				.toArray(double[][]::new);
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("ea.csv"))) {
			for (double[] doubles : generation) {
				writer.write(String.format("%f,%f%n", doubles[0], doubles[1]));
			}
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void resetLearningImpl() {
		dataProvider = new DataProvider(LIMIT, TRAINING_TO_ALL_RATIO);
	}

	@Override
	public void trainOnInstanceImpl(com.yahoo.labs.samoa.instances.Instance inst) {
		c = Math.max(c, (int) inst.classValue());
		dataProvider.add(inst);
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		return new Measurement[0];
	}

	@Override
	public void getModelDescription(StringBuilder out, int indent) {

	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public ImmutableCapabilities defineImmutableCapabilities() {
		if (this.getClass() == HybridEvolutionaryAlgorithm.class)
			return new ImmutableCapabilities(Capability.VIEW_STANDARD, Capability.VIEW_LITE);
		else
			return new ImmutableCapabilities(Capability.VIEW_STANDARD);
	}
}