package moa.classifiers.lazy;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

public class InstanceProvider {

    private static final double TRAINING_TO_ALL_RATIO = 0.6;

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
            } else {
                trainingInstances.delete(0);
                trainingInstances.add(testInstances.get(0));
                testInstances.delete(0);
                testInstances.add(instance);
            }
        } else {
            trainingInstances.add(instance);
        }
        instances.add(instance);
        instancesProcessed++;
        if (instances.numInstances() > limit) {
            instances.delete(0);
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
