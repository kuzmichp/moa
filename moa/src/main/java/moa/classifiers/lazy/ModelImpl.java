package moa.classifiers.lazy;

import static io.jenetics.engine.EvolutionResult.toBestEvolutionResult;
import static io.jenetics.engine.EvolutionResult.toUniquePopulation;
import static moa.classifiers.lazy.InstanceType.TEST;
import static moa.classifiers.lazy.InstanceType.TRAINING;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.EvictingQueue;
import com.yahoo.labs.samoa.instances.Instance;

import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.Limits;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;
import smile.classification.KNN;
import smile.math.distance.EuclideanDistance;
import smile.math.distance.HammingDistance;

@SuppressWarnings("UnstableApiUsage")
public class ModelImpl implements Model {

	private final int populationSize;
	private final int maxEpochs;
	private final int steadiness;
	private final int k;
	private int c;

	private final EvictingQueue<Instance> buffer;
	private final Splitter splitter;

	private Genotype<DoubleGene> genotype;

	private ModelImpl(int populationSize, int maxEpochs, int steadiness, int limit, int k) {
		this.populationSize = populationSize;
		this.maxEpochs = maxEpochs;
		this.steadiness = steadiness;
		this.k = k;
		this.buffer = EvictingQueue.create(limit);
		this.splitter = new SplitterImpl();
	}

	public static class Builder {

		private int populationSize;
		private int maxEpochs;
		private int steadiness;
		private int k;
		private int limit;

		public static Builder create() {
			return new Builder();
		}

		public Model build() {
			return new ModelImpl(populationSize, maxEpochs, steadiness, limit, k);
		}

		public Builder populationSize(int populationSize) {
			this.populationSize = populationSize;
			return this;
		}

		public Builder maxEpochs(int maxEpochs) {
			this.maxEpochs = maxEpochs;
			return this;
		}

		public Builder steadiness(int steadiness) {
			this.steadiness = steadiness;
			return this;
		}

		public Builder k(int k) {
			this.k = k;
			return this;
		}

		public Builder limit(int limit) {
			this.limit = limit;
			return this;
		}
	}

	@Override
	public double[] predict(Instance instance) {
		Map<InstanceType, List<Instance>> split = splitter.split(buffer);
		if (genotype == null) {
			System.out.println("Creating model from scratch...");
			EvolutionResult<DoubleGene, Double> evolutionResult = createModel(instance.numInputAttributes(),
					split.get(TRAINING), split.get(TEST));
			genotype = evolutionResult.getBestPhenotype().getGenotype();
		}
		return getVotes(instance);
	}

	private EvolutionResult<DoubleGene, Double> createModel(int individualSize, List<Instance> training,
			List<Instance> test) {
		Factory<Genotype<DoubleGene>> factory = Genotype.of(DoubleChromosome.of(0, 1, individualSize));
		Engine<DoubleGene, Double> engine = Engine.builder(g -> eval(g, training, test), factory)
				.mapping(toUniquePopulation())
				.populationSize(populationSize)
				.build();
		EvolutionStatistics<Double, DoubleMomentStatistics> statistics = EvolutionStatistics.ofNumber();
		EvolutionResult<DoubleGene, Double> evolutionResult = engine.stream().limit(Limits.bySteadyFitness(steadiness))
				.limit(maxEpochs)
				.peek(statistics)
				.peek(this::monitor)
				.collect(toBestEvolutionResult());
		System.out.println(statistics);
		return evolutionResult;
	}

	private double[] getVotes(Instance instance) {
		double[] weights = genotype.getChromosome().stream().mapToDouble(DoubleGene::doubleValue).toArray();
		double[] votes = new double[c + 1];
		try {
			KNN<double[]> knn = KNN.fit(getAttributes(buffer), getClasses(buffer), new EuclideanDistance(weights), k);
			int prediction = knn.predict(Arrays.copyOf(instance.toDoubleArray(), instance.numInputAttributes()));
			votes[prediction] = 1;
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
		}
		return votes;
	}

	private double eval(Genotype<DoubleGene> genotype, List<Instance> training, List<Instance> test) {
		double[] weights = genotype.getChromosome().stream().mapToDouble(DoubleGene::doubleValue).toArray();
		try {
			KNN<double[]> knn = KNN.fit(getAttributes(training), getClasses(training), new EuclideanDistance(weights), k);
			return computeFitness(getClasses(test), knn.predict(getAttributes(test)));
		} catch (IllegalArgumentException e) {
			System.out.println(e.getMessage());
			return 0.0;
		}
	}

	private double computeFitness(int[] expected, int[] predicted) {
		int hits = expected.length - HammingDistance.d(expected, predicted);
		return (double) hits / expected.length;
	}

	private void monitor(EvolutionResult<DoubleGene, Double> evolutionResult) {
		double[] weights = evolutionResult.getBestPhenotype()
				.getGenotype()
				.getChromosome()
				.stream()
				.mapToDouble(DoubleGene::doubleValue).toArray();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("ea.csv", true))) {
			writer.append(String.format("%f,%f%n",weights[0], weights[1]));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int[] getClasses(Collection<Instance> instances) {
		return instances.stream().mapToInt(i -> (int) i.classValue()).toArray();
	}

	private double[][] getAttributes(Collection<Instance> instances) {
		return instances.stream()
				.map(i -> Arrays.copyOf(i.toDoubleArray(), i.numInputAttributes()))
				.toArray(double[][]::new);
	}

	@Override
	public void train(Instance instance) {
		buffer.add(instance);
		c = Math.max(c, (int) instance.classValue());
	}
}
