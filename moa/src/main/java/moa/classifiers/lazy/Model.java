package moa.classifiers.lazy;

import com.yahoo.labs.samoa.instances.Instance;

public interface Model {

	double[] predict(Instance instance);
	void train(Instance instance);
}
