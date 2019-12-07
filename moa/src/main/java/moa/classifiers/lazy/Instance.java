package moa.classifiers.lazy;

import java.util.Arrays;

public final class Instance {

	private final double[] attributes;
	private final int clazz;

	public Instance(double[] attributes, int clazz) {
		this.attributes = attributes;
		this.clazz = clazz;
	}

	public Instance(com.yahoo.labs.samoa.instances.Instance instance) {
		this.attributes = Arrays.copyOf(instance.toDoubleArray(), instance.numInputAttributes());
		this.clazz = (int) instance.classValue();
	}

	public double[] getAttributes() {
		return attributes;
	}

	public int getClazz() {
		return clazz;
	}
}
