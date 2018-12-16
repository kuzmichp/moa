package moa.classifiers.lazy;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;
import org.apache.commons.math3.genetics.GeneticAlgorithm;
import org.apache.commons.math3.random.RandomGenerator;

import java.util.*;

public class InstanceProvider {

    private static final double TRAINING_TO_ALL_RATIO = 0.6;
    private static final double APPROXIMATION_RATE = 0.01;

    private final HashMap<Double, Integer> overallClassDistribution;
    private final HashMap<Double, List<Instance>> testInstanceGroups;
    private final HashMap<Double, List<Instance>> trainingInstanceGroups;

    private Instances instances;
    private Instances trainingInstances;
    private Instances testInstances;

    private int instancesProcessed;
    private final int limit;
    private final int trainingSetSize;

    InstanceProvider(InstancesHeader context, int limit) {
        this.limit = limit;
        trainingSetSize = (int) Math.ceil(limit * TRAINING_TO_ALL_RATIO);
        initialize(context);
        overallClassDistribution = new HashMap<>();
        testInstanceGroups = new HashMap<>();
        trainingInstanceGroups = new HashMap<>();
    }

    public void initialize(InstancesHeader context) {
        instances = new Instances(context, 0);
        instances.setClassIndex(context.classIndex());

        trainingInstances = new Instances(context, 0);
        trainingInstances.setClassIndex(context.classIndex());

        testInstances = new Instances(context, 0);
        testInstances.setClassIndex(context.classIndex());
    }

    public void add(Instance instance) {
        if (isTrainingSetFull()) {
            if (!isTestSetFull()) {
                testInstances.add(instance);
                double instanceClass = instance.classValue();
                List<Instance> existingInstances = testInstanceGroups.containsKey(instanceClass)
                        ? testInstanceGroups.get(instanceClass)
                        : new ArrayList<>();
                existingInstances.add(instance);
                testInstanceGroups.put(instanceClass, existingInstances);
            } else {
                trainingInstances.delete(0);

                Instance dumped = trainingInstances.get(0);
                double clazz = dumped.classValue();
                List<Instance> existing = trainingInstanceGroups.get(clazz);
                existing.remove(dumped);
                trainingInstanceGroups.put(clazz, existing);


                trainingInstances.add(testInstances.get(0));

                double instanceClass = testInstances.get(0).classValue();
                List<Instance> existingInstances = testInstanceGroups.get(instanceClass);
                existingInstances.remove(testInstances.get(0));
                testInstanceGroups.put(instanceClass, existingInstances);

                testInstances.delete(0);
                testInstances.add(instance);
            }
        } else {
            trainingInstances.add(instance);
            double clazz = instance.classValue();
            List<Instance> existingInstances = trainingInstanceGroups.containsKey(clazz)
                    ? trainingInstanceGroups.get(clazz)
                    : new ArrayList<>();
            existingInstances.add(instance);
            trainingInstanceGroups.put(clazz, existingInstances);
        }
        instances.add(instance);
        updateOverallDistribution(instance, true);
        instancesProcessed++;
        if (instances.numInstances() > limit) {
            updateOverallDistribution(instances.get(0), false);
            instances.delete(0);
        }
    }

    public Instances getApproximatedTrainingInstances() {
        int min = Collections.min(overallClassDistribution.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getValue();
        double minClazz = Collections.min(overallClassDistribution.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
        int maxClass = Collections.max(overallClassDistribution.keySet()).intValue();
        double[] rates = new double[maxClass + 1];
        for (Map.Entry<Double, Integer> entry : overallClassDistribution.entrySet()) {
            int clazz = entry.getKey().intValue();
            int count = entry.getValue();
            rates[clazz] = count * 1.0 / min;
        }
        int minSize = (int) Math.floor(trainingInstanceGroups.get(minClazz).size() * APPROXIMATION_RATE);
        Instances result = new Instances(trainingInstances);
        result.delete();
        RandomGenerator generator = GeneticAlgorithm.getRandomGenerator();
        for (Map.Entry<Double, List<Instance>> entry : trainingInstanceGroups.entrySet()) {
            int clazz = entry.getKey().intValue();
            List<Instance> instances = entry.getValue();
            int reducedCount = (int) Math.floor(minSize * rates[clazz]);
            for (int i = 0; i < reducedCount; i++) {
                int index = generator.nextInt(instances.size());
                result.add(instances.get(index));
            }
        }
        Random random = new Random(13);
        result.randomize(random);
        return result;
    }

    public Instances getApproximatedTestInstances() {
        Instances result = new Instances(testInstances);
        result.delete();
        RandomGenerator generator = GeneticAlgorithm.getRandomGenerator();
        for (Map.Entry<Double, List<Instance>> entry : testInstanceGroups.entrySet()) {
            List<Instance> instances = entry.getValue();
            int reducedCount = (int) Math.floor(instances.size() * APPROXIMATION_RATE);
            for (int i = 0; i < reducedCount; i++) {
                int index = generator.nextInt(instances.size());
                result.add(instances.get(index));
            }
        }
        Random random = new Random(13);
        result.randomize(random);
        return result;
    }

    private void updateOverallDistribution(Instance instance, boolean add) {
        double classValue = instance.classValue();
        if (add) {
            if (overallClassDistribution.containsKey(classValue)) {
                overallClassDistribution.put(classValue, overallClassDistribution.get(classValue) + 1);
            } else {
                overallClassDistribution.put(classValue, 1);
            }
        } else {
            if (overallClassDistribution.containsKey(classValue)) {
                overallClassDistribution.put(classValue, overallClassDistribution.get(classValue) - 1);
            }
        }
    }

    boolean isEvolvementNeeded() {
        return instancesProcessed % limit == 0;
    }

    private boolean isTestSetFull() {
        return testInstances.size() == limit - trainingSetSize;
    }

    private boolean isTrainingSetFull() {
        return trainingInstances.size() == trainingSetSize;
    }

    public Instances getInstances() {
        return instances;
    }

    public Instances getTrainingInstances() {
        return trainingInstances;
    }

    public Instances getTestInstances() {
        return testInstances;
    }
}
