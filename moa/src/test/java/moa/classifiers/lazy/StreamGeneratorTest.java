package moa.classifiers.lazy;

import java.awt.*;
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

	private final Color[] colors = {
			new Color(240, 163, 255),
			new Color(0, 117, 220),
			new Color(153, 63, 0),
			new Color(76, 0, 92),
			new Color(25, 25, 25),
			new Color(0, 92, 49),
			new Color(43, 206, 72),
			new Color(255, 204, 153),
			new Color(128, 128, 128),
			new Color(148, 255, 181),
			new Color(143, 124, 0),
			new Color(157, 204, 0),
			new Color(194, 0, 136),
			new Color(0, 51, 128),
			new Color(255, 164, 5),
			new Color(255, 168, 187),
			new Color(66, 102, 0),
			new Color(255, 0, 16),
			new Color(94, 241, 242),
			new Color(0, 153, 143),
			new Color(224, 255, 102),
			new Color(116, 10, 255),
			new Color(153, 0, 0),
			new Color(255, 255, 128),
			new Color(255, 255, 0),
			new Color(255, 80, 5)
	};

	@Test
	public void shouldGenerateStream() throws IOException {
		generator.generate(10200, new FileWriter("10200.csv"));
	}

	@Test
	public void shouldVisualize() throws IOException {
		List<double[]> x = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader("ea.csv"))) {
			reader.lines().limit(100).forEach(l -> {
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

	@Test
	public void shouldVisualizePopulations() throws IOException {
		PlotCanvas canvas = new PlotCanvas(new double[] { 0.0, 0.0 }, new double[] { 1.0, 1.0 });
		int populationSize = 100;
		int epochs = 100;
		try (BufferedReader reader = new BufferedReader(new FileReader("populations.csv"))) {
			for (int e = 0; e < epochs; e++) {
				double[][] population = new double[populationSize][];
				for (int i = 0; i < populationSize; i++) {
					double[] individual = Arrays.stream(reader.readLine().split(","))
							.mapToDouble(Double::parseDouble)
							.toArray();
					population[i] = individual;
				}
				if (e >= 8) {
					continue;
				}
				ScatterPlot plot = new ScatterPlot(population, '#', colors[e]);
				plot.setID(String.valueOf(e));
				canvas.add(plot);
			}
		}
		Headless headless = new Headless(canvas);
		headless.pack();
		headless.setVisible(true);
		canvas.save(new File("evolution.png"));
	}
}
