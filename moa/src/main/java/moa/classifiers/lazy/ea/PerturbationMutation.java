package moa.classifiers.lazy.ea;

import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.MutationPolicy;

import java.util.ArrayList;
import java.util.List;

public class PerturbationMutation implements MutationPolicy {

    @Override
    public Chromosome mutate(Chromosome original) {
        if (!(original instanceof RealChromosome)) {
            String message = String.format("Unable to mutate chromosome of type %s", original.getClass().getSimpleName());
            throw new IllegalArgumentException(message);
        }

        RealChromosome realChromosome = (RealChromosome) original;
        List<Double> representation = realChromosome.getRepresentation();

        CauchyDistribution distribution = new CauchyDistribution(0, 1);
        double[] sample = distribution.sample(realChromosome.getLength());
        List<Double> mutant = new ArrayList<>(representation);
        for (int i = 0; i < sample.length; i++) {
            double gene = mutant.get(i) + sample[i];
            if (gene >= 0 && gene <= 1) {
                mutant.set(i, gene);
            } else {
                mutant.set(i, gene < 0 ? 0. : 1);
            }
        }

        return realChromosome.newFixedLengthChromosome(mutant);
    }
}
