package moa.classifiers.hybrid;

import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.Instance;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.classifiers.hybrid.splitter.SplitterImpl;
import moa.core.Measurement;

public class HybridEvolutionaryAlgorithm extends AbstractClassifier implements MultiClassClassifier,
		CapabilitiesHandler {

	public static final IntOption kOption = new IntOption("k", 'k', "The number of neighbors", 30, 1, Integer.MAX_VALUE);
	public static final IntOption lOption = new IntOption("limit", 'l', "The maximum number of instances to store", 1000, 1, Integer.MAX_VALUE);

	// EA parameters
	public static final IntOption pOption = new IntOption("p", 'p', "The population size", 50, 10, Integer.MAX_VALUE);
	public static final IntOption eOption = new IntOption("e", 'e', "The number of epochs", 100, 1, Integer.MAX_VALUE);
	public static final IntOption sOption = new IntOption("s", 's', "", 10, 1, Integer.MAX_VALUE);
	public static final IntOption fOption = new IntOption("f", 'f', "", 10000, 1, Integer.MAX_VALUE);

	private transient Model model;

	@Override
	public double[] getVotesForInstance(Instance inst) {
		return model.predict(inst);
	}

	@Override
	public void resetLearningImpl() {
		model = Model.Builder.create()
				.freshnessThreshold(fOption.getValue())
				.k(kOption.getValue())
				.limit(lOption.getValue())
				.maxEpochs(eOption.getValue())
				.populationSize(pOption.getValue())
				.splitter(new SplitterImpl())
				.steadiness(sOption.getValue())
				.build();
	}

	@Override
	public void trainOnInstanceImpl(Instance inst) {
		model.train(inst);
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
