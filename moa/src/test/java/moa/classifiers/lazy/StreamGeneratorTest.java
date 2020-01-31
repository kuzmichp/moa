package moa.classifiers.lazy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import smile.plot.Headless;
import smile.plot.PlotCanvas;
import smile.plot.ScatterPlot;

public class StreamGeneratorTest {

	private final StreamGenerator generator = new StreamGenerator();

//	@Test
//	public void shouldGenerateStream() throws IOException {
//		generator.generate(1050, new FileWriter("1050.csv"));
//	}

	@Test
	public void shouldVisualize() throws IOException {
		List<double[]> x = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader("ea.csv"))) {
			reader.lines().limit(98).forEach(l -> {
				double[] weights = Arrays.stream(l.split(","))
						.mapToDouble(Double::parseDouble)
						.toArray();
				x.add(weights);
			});
		}
		visualize(x.stream().toArray(double[][]::new));
	}

	private void visualize(double[][] x) throws IOException {
		PlotCanvas scatterPlot = ScatterPlot.plot(x, '#');
		Headless headless = new Headless(scatterPlot);
		headless.pack();
		headless.setVisible(true);
		scatterPlot.save(new File("best-individuals.png"));
	}
}
