package moa.classifiers.lazy;

import static java.awt.Color.BLUE;
import static java.awt.Color.RED;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import smile.plot.Headless;
import smile.plot.PlotCanvas;
import smile.plot.ScatterPlot;

public class StreamGenerator {

	private final Random rand = new Random();

	public void generate(int n, FileWriter fileWriter) throws IOException {
		double[][] x = new double[n][];
		int[] y = new int[n];
		try (BufferedWriter writer = new BufferedWriter(fileWriter)) {
			writer.write("x1,x2,y\n");
			for (int i = 0; i < n; i++) {
				double x1 = rand.nextDouble();
				double x2 = rand.nextDouble();
				x[i] = new double[] { x1, x2 };
				y[i] = x1 <= 0.5 ? 0 : 1;
				String instance = String.format("%f,%f,%d%n", x1, x2, y[i]);
				writer.write(instance);
			}
		}
		String path = String.format("%d.png", n);
		PlotCanvas scatterPlot = ScatterPlot.plot(x, y, new char[] { '*', '#' }, new Color[] { RED, BLUE});
		Headless headless = new Headless(scatterPlot);
		headless.pack();
		headless.setVisible(true);
		scatterPlot.save(new File(path));
	}
}
