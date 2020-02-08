package moa.classifiers.lazy;

import static moa.classifiers.lazy.InstanceType.TEST;
import static moa.classifiers.lazy.InstanceType.TRAINING;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableMap;
import com.yahoo.labs.samoa.instances.Instance;

@SuppressWarnings("UnstableApiUsage")
public class SplitterImpl implements Splitter {

	private static final double RATIO = 0.7;

	@Override
	public Map<InstanceType, List<Instance>> split(EvictingQueue<Instance> buffer) {
		List<Instance> instances = Arrays.asList(buffer.toArray(new Instance[0]));
		Collections.shuffle(instances);
		int trainingSize = (int) Math.ceil(instances.size() * RATIO);
		return ImmutableMap.of(TRAINING, instances.subList(0, trainingSize),
				TEST, instances.subList(trainingSize, instances.size()));
	}
}
