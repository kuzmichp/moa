package moa.classifiers.hybrid;

import static io.jenetics.engine.EvolutionResult.toBestEvolutionResult;
import static io.jenetics.engine.EvolutionResult.toUniquePopulation;
import static moa.classifiers.hybrid.InstanceType.TEST;
import static moa.classifiers.hybrid.InstanceType.TRAINING;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.EvictingQueue;
import com.yahoo.labs.samoa.instances.Instance;

import de.erichseifert.gral.data.DataTable;
import io.jenetics.DoubleChromosome;
import io.jenetics.DoubleGene;
import io.jenetics.Genotype;
import io.jenetics.Phenotype;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.engine.EvolutionStatistics;
import io.jenetics.engine.EvolutionStream;
import io.jenetics.engine.Limits;
import io.jenetics.stat.DoubleMomentStatistics;
import io.jenetics.util.Factory;
import io.jenetics.util.ISeq;
import moa.classifiers.hybrid.splitter.Splitter;
import smile.classification.KNN;
import smile.math.distance.EuclideanDistance;
import smile.math.distance.HammingDistance;

@SuppressWarnings("UnstableApiUsage")
public class Model {

	private int c;

	private final int populationSize;
	private final int maxEpochs;
	private final int steadiness;
	private final int freshnessThreshold;
	private final int k;

	private ISeq<Phenotype<DoubleGene, Double>> population;
	private Genotype<DoubleGene> genotype;
	private EvictingQueue<Instance> buffer;
	private Splitter splitter;
	private int processedSinceLastUpdate;

	List<DataTable> dataTables = new ArrayList<>();

	private Model(int freshnessThreshold, int steadiness, int maxEpochs, int populationSize, int k, int limit, Splitter splitter) {
		this.freshnessThreshold = freshnessThreshold;
		this.steadiness = steadiness;
		this.maxEpochs = maxEpochs;
		this.populationSize = populationSize;
		this.k = k;
		this.buffer = EvictingQueue.create(limit);
		this.splitter = splitter;
	}

	public static class Builder {

		private int populationSize = 100;
		private int maxEpochs = 100;
		private int steadiness = 10;
		private int freshnessThreshold = 1500;
		private int k = 30;
		private int limit = 1000;
		private Splitter splitter;

		public static Builder create() {
			return new Builder();
		}

		public Model build() {
			return new Model(freshnessThreshold, steadiness, maxEpochs, populationSize, k, limit, splitter);
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

		public Builder freshnessThreshold(int freshnessThreshold) {
			this.freshnessThreshold = freshnessThreshold;
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

		public Builder splitter(Splitter splitter) {
			this.splitter = splitter;
			return this;
		}
	}

	public double[] predict(Instance instance) {
		Map<InstanceType, List<Instance>> split = splitter.split(buffer);
		if (isOutdated() || population == null) {
			String outdated = String.format("Model is outdated as number of processed examples since last update %d"
							+ " is greater than freshness threshold %d. Updating model...", processedSinceLastUpdate,
					freshnessThreshold);
			System.out.println(isOutdated() ? outdated : "Creating model from scratch...");
			update(instance.numInputAttributes(), split.get(TRAINING), split.get(TEST));
		}
		return getVotes(instance, c);
	}

	private double[] getVotes(Instance instance, int c) {
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

	private void update(int individualSize, List<Instance> training, List<Instance> test) {
		Factory<Genotype<DoubleGene>> factory = Genotype.of(DoubleChromosome.of(0, 1, individualSize));
		Engine<DoubleGene, Double> engine = Engine.builder(g -> eval(g, training, test), factory)
				.mapping(toUniquePopulation())
				.populationSize(populationSize)
				.build();
		EvolutionStream<DoubleGene, Double> stream = population == null ? engine.stream() : engine.stream(population);
		EvolutionStatistics<Double, DoubleMomentStatistics> statistics = EvolutionStatistics.ofNumber();
		EvolutionResult<DoubleGene, Double> result = stream.limit(Limits.bySteadyFitness(steadiness))
				.limit(maxEpochs)
				.peek(statistics)
				.peek(this::consumeOneGenerationResult)
				.collect(toBestEvolutionResult());
		population = result.getPopulation();
		genotype = result.getBestPhenotype().getGenotype();
		System.out.println(statistics);
		processedSinceLastUpdate = 0;
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

	private int[] getClasses(Collection<Instance> instances) {
		return instances.stream().mapToInt(i -> (int) i.classValue()).toArray();
	}

	private double[][] getAttributes(Collection<Instance> instances) {
		return instances.stream()
				.map(i -> Arrays.copyOf(i.toDoubleArray(), i.numInputAttributes()))
				.toArray(double[][]::new);
	}

	private double computeFitness(int[] expected, int[] predicted) {
		int hits = expected.length - HammingDistance.d(expected, predicted);
		return (double) hits / expected.length;
	}

	/**
	 * Adds instance to buffer.
	 * @param instance training instance from stream
	 */
	public void train(Instance instance) {
		buffer.add(instance);
		processedSinceLastUpdate++;
		c = Math.max(c, (int) instance.classValue());
	}

	private boolean isOutdated() {
		return processedSinceLastUpdate > freshnessThreshold;
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
}
