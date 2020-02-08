package moa.classifiers.lazy;

import java.util.List;
import java.util.Map;

import com.google.common.collect.EvictingQueue;
import com.yahoo.labs.samoa.instances.Instance;

@SuppressWarnings("UnstableApiUsage")
public interface Splitter {

	/**
	 * Splits data to training and test.
	 * @param buffer all instances
	 * @return map with training instances under TRAINING key and test instances under TEST
	 */
	Map<InstanceType, List<Instance>> split(EvictingQueue<Instance> buffer);
}
