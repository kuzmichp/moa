package moa.classifiers.lazy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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

	@Test
	public void shouldGenerateStream() throws IOException {
		generator.generate(10200, new FileWriter("10200.csv"));
	}

	@Test
	public void shouldVisualize() throws IOException {
		List<double[]> x = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader("ea.csv"))) {
			reader.lines().limit(16).forEach(l -> {
				double[] weights = Arrays.stream(l.split(","))
						.mapToDouble(Double::parseDouble)
						.toArray();
				x.add(weights);
			});
		}
//		x.add(new double[] {1., 1.});
//		x.add(new double[] {0., 0.});
		visualize(x.stream().toArray(double[][]::new));
	}

	private void visualize(double[][] x) throws IOException {
		PlotCanvas canvas = new PlotCanvas(new double[] { 0.0, 0.0 }, new double[] { 1.0, 1.0 });
		ScatterPlot plot = new ScatterPlot(x, '#');
		plot.setID("1");
		canvas.add(plot);
		Headless headless = new Headless(canvas);
		headless.pack();
		headless.setVisible(true);
		canvas.save(new File("best-individuals.png"));
	}
}
