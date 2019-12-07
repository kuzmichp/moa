package moa.classifiers.lazy;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;

public class DataProvider {

	private final LinkedList<Instance> instances;

	private List<Instance> training;
	private List<Instance> test;

	private final int limit;
	private final double ratio;

	/**
	 * Initializes a data provider with a buffer size and a training to all instances ratio.
	 *
	 * @param limit the buffer size
	 * @param ratio the training to all instances ratio
	 */
	public DataProvider(int limit, double ratio) {
		this.instances = new LinkedList<>();
		this.limit = limit;
		this.ratio = ratio;
	}

	/**
	 * Adds an instance from a stream to the buffer.
	 *
	 * @param instance the instance from the stream
	 */
	public void add(com.yahoo.labs.samoa.instances.Instance instance) {
		if (instances.size() == limit) {
			instances.removeFirst();
		}
		instances.addLast(new Instance(instance));
	}

	public LearningData getLearningData() {
		return new LearningData(instances, ratio);
	}

	public static class LearningData {

		private final double[][] allAttributes;
		private final double[][] trainingAttributes;
		private final double[][] testAttributes;
		private final int[] allClasses;
		private final int[] trainingClasses;
		private final int[] testClasses;

		public LearningData(LinkedList<Instance> instances, double ratio) {
			List<Instance> shuffled = Lists.newArrayList(instances);
			Collections.shuffle(shuffled);
			allAttributes = getAttributes(shuffled);
			allClasses = getClasses(shuffled);
			int trainingSetSize = (int) Math.ceil(instances.size() * ratio);
			List<Instance> training = shuffled.subList(0, trainingSetSize);
			List<Instance> test = shuffled.subList(trainingSetSize, instances.size());
			trainingAttributes = getAttributes(training);
			testAttributes = getAttributes(test);
			trainingClasses = getClasses(training);
			testClasses = getClasses(test);
		}

		public double[][] getTrainingAttributes() {
			return trainingAttributes;
		}

		public double[][] getTestAttributes() {
			return testAttributes;
		}

		public int[] getTrainingClasses() {
			return trainingClasses;
		}

		public int[] getTestClasses() {
			return testClasses;
		}

		public double[][] getAttributes(List<Instance> instances) {
			return instances.stream().map(Instance::getAttributes).toArray(double[][]::new);
		}

		public int[] getClasses(List<Instance> instances) {
			return instances.stream().mapToInt(Instance::getClazz).toArray();
		}

		public double[][] getAllAttributes() {
			return allAttributes;
		}

		public int[] getAllClasses() {
			return allClasses;
		}
	}
}
