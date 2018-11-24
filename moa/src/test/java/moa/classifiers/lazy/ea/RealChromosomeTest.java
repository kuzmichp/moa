package moa.classifiers.lazy.ea;

import moa.classifiers.lazy.InstanceProvider;
import moa.classifiers.lazy.neighboursearch.NearestNeighbourSearch;
import org.apache.commons.math3.genetics.Chromosome;
import org.apache.commons.math3.genetics.Population;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.Collections;

import static org.mockito.Mockito.*;

public class RealChromosomeTest {

    @Mock
    private InstanceProvider instanceProvider;
    @Mock
    private Population population;
    @Mock
    private NearestNeighbourSearch neighbourSearch;
    @Mock
    private FitnessCache<Chromosome, Double> cache;
    @Spy
    private Chromosome chromosome =
            new RealChromosome(Collections.singletonList(1.), instanceProvider, cache, population, neighbourSearch, 1);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doReturn(2.).when(chromosome).fitness();
    }

    @Test
    public void shouldCalculateFitnessOnce() {
        // when
        chromosome.getFitness();
        chromosome.getFitness();

        // then
        verify(chromosome, times(1)).fitness();
    }
}
