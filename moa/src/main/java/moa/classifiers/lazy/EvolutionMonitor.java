package moa.classifiers.lazy;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import io.jenetics.DoubleGene;
import io.jenetics.Phenotype;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.ISeq;

public class EvolutionMonitor {

	public void monitorBestGenotype(EvolutionResult<DoubleGene, Double> evolutionResult) {
		double[] weights = convertToDoubleArray(evolutionResult.getBestPhenotype());
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("moa/ea.csv", true))) {
			for (double weight : weights) {
				writer.append(String.format("%f,", weight));
			}
			writer.append(String.format("%n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void monitorPopulation(EvolutionResult<DoubleGene, Double> evolutionResult) {
		ISeq<Phenotype<DoubleGene, Double>> population = evolutionResult.getPopulation();
		double[][] phenotypes = new double[population.size()][];
		for (int i = 0; i < population.size(); i++) {
			phenotypes[i] = convertToDoubleArray(population.get(i));
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter("moa/populations.csv", true))) {
			for (double[] phenotype : phenotypes) {
				for (double weight : phenotype) {
					writer.append(String.format("%f,", weight));
				}
				writer.append(String.format("%n"));
			}
			//writer.append(String.format("%%%n"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private double[] convertToDoubleArray(Phenotype<DoubleGene, Double> phenotype) {
		return phenotype.getGenotype()
				.getChromosome()
				.stream()
				.mapToDouble(DoubleGene::doubleValue).toArray();
	}
}
