package moa.classifiers.hybrid.splitter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.EvictingQueue;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstanceImpl;

import moa.classifiers.hybrid.InstanceType;

@SuppressWarnings("UnstableApiUsage")
@RunWith(Parameterized.class)
public class SplitterImplTest {

	private final Splitter splitter = new SplitterImpl();

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ createBuffer(3), 3, 0 },
				{ createBuffer(5), 4, 1 },
				{ createBuffer(10), 7, 3 },
		});
	}

	private static EvictingQueue<Instance> createBuffer(int size) {
		EvictingQueue<Instance> buffer = EvictingQueue.create(size);
		List<InstanceImpl> instances = IntStream.rangeClosed(1, size).mapToObj(i -> new InstanceImpl(1))
				.collect(Collectors.toList());
		buffer.addAll(instances);
		return buffer;
	}

	private EvictingQueue<Instance> buffer;
	private int expectedTrainingSetSize;
	private int expectedTestSetSize;

	public SplitterImplTest(EvictingQueue<Instance> buffer, int expectedTrainingSetSize, int expectedTestSetSize) {
		this.buffer = buffer;
		this.expectedTrainingSetSize = expectedTrainingSetSize;
		this.expectedTestSetSize = expectedTestSetSize;
	}

	@Test
	public void shouldSplitIntoChunksWithCorrectSize() {
		// when
		Map<InstanceType, java.util.List<Instance>> split = splitter.split(buffer);

		// then
		assertEquals(expectedTrainingSetSize, split.get(InstanceType.TRAINING).size());
		assertEquals(expectedTestSetSize, split.get(InstanceType.TEST).size());
	}
}
