package moa.classifiers.lazy;

import com.yahoo.labs.samoa.instances.Instance;

public class Ensemble implements Model {

	@Override
	public double[] predict(Instance instance) {
		return new double[0];
	}

	@Override
	public void train(Instance instance) {

	}
}
