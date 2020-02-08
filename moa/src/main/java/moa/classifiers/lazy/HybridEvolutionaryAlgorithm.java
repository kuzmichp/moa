package moa.classifiers.lazy;

import moa.capabilities.CapabilitiesHandler;
import moa.capabilities.Capability;
import moa.capabilities.ImmutableCapabilities;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.MultiClassClassifier;
import moa.core.Measurement;

public class HybridEvolutionaryAlgorithm extends AbstractClassifier implements MultiClassClassifier,
		CapabilitiesHandler {

	private transient Model model;

	@Override
	public double[] getVotesForInstance(com.yahoo.labs.samoa.instances.Instance inst) {
		return model.predict(inst);
	}

	@Override
	public void trainOnInstanceImpl(com.yahoo.labs.samoa.instances.Instance inst) {
		model.train(inst);
	}

	@Override
	public void resetLearningImpl() {
		model = ModelImpl.Builder.create()
				.populationSize(100)
				.maxEpochs(100)
				.steadiness(10)
				.limit(1000)
				.k(30)
				.build();
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
