package moa.classifiers.meta;

import weka.core.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import moa.classifiers.AbstractClassifier;
import moa.classifiers.Classifier;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.options.ClassOption;
import moa.options.FlagOption;
import moa.options.IntOption;
import moa.options.ListOption;
import moa.options.Option;
import moa.tasks.TaskMonitor;

public abstract class BlastAbstract extends AbstractClassifier {
	
	private static final long serialVersionUID = 1L;
	
	public ListOption baselearnersOption = new ListOption(
			"baseClassifiers", 'b', "The classifiers the ensemble consists of.", 
			new ClassOption("learner", ' ', "", Classifier.class, "trees.HoeffdingTree"), 
			new Option[]{
                new ClassOption("", ' ', "", Classifier.class, "bayes.NaiveBayes"), 
                new ClassOption("", ' ', "", Classifier.class, "functions.Perceptron"),
                new ClassOption("", ' ', "", Classifier.class, "functions.SGD"),
                new ClassOption("", ' ', "", Classifier.class, "lazy.kNN"),
                new ClassOption("", ' ', "", Classifier.class, "trees.HoeffdingTree")},
           ',');
	
	public IntOption gracePerionOption = new IntOption(
            "gracePeriod",
            'g',
            "How many instances before we reevalate the best classifier",
            1, 1, Integer.MAX_VALUE);
	
	public IntOption activeClassifiersOption = new IntOption(
			"activeClassifiers",
			'k',
			"The number of active classifiers (used for voting)",
			1, 1, Integer.MAX_VALUE);
	
	public FlagOption weightClassifiersOption = new FlagOption(
			"weightClassifiers", 
			'p',
			"Uses online performance estimation to weight the classifiers");
	
	protected Classifier[] ensemble;
    
    protected double[] historyTotal;
    
    protected Integer instancesSeen;
    
    List<Integer> topK;
	
	@Override
	public double[] getVotesForInstance(Instance inst) {
		double[] votes = new double[inst.classAttribute().numValues()];
		
		for (int i = 0; i < topK.size(); ++i) {
			double[] memberVotes = normalize(ensemble[topK.get(i)].getVotesForInstance(inst));
			double weight = 1.0;
			
			if (weightClassifiersOption.isSet()) {
				weight = historyTotal[topK.get(i)];
			}
			
			// make internal classifiers so-called "hard classifiers"
			votes[maxIndex(memberVotes)] += 1.0 * weight;
		}
		
		return votes;
	}

	@Override
	public boolean isRandomizable() {
		return false;
	}

	@Override
	public void getModelDescription(StringBuilder arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
    public void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
		
        Option[] learnerOptions = this.baselearnersOption.getList();
        this.ensemble = new Classifier[learnerOptions.length];
        for (int i = 0; i < learnerOptions.length; i++) {
        	monitor.setCurrentActivity("Materializing learner " + (i + 1)
                    + "...", -1.0);
            this.ensemble[i] = (Classifier) ((ClassOption) learnerOptions[i]).materializeObject(monitor, repository);
            if (monitor.taskShouldAbort()) {
                return;
            }
            monitor.setCurrentActivity("Preparing learner " + (i + 1) + "...",
                    -1.0);
            this.ensemble[i].prepareForUse(monitor, repository);
            if (monitor.taskShouldAbort()) {
                return;
            }
        }
        super.prepareForUseImpl(monitor, repository);
        
        topK = topK(historyTotal, activeClassifiersOption.getValue());
    }
	
	protected static List<Integer> topK(double[] scores, int k) {
		double[] scoresWorking = Arrays.copyOf(scores, scores.length);
		
		List<Integer> topK = new ArrayList<Integer>();
		
		for (int i = 0; i < k; ++i) {
			int bestIdx = maxIndex(scoresWorking);
			topK.add(bestIdx);
			scoresWorking[bestIdx] = -1;
		}
		
		return topK;
	}
	
	protected static int maxIndex(double[] scores) {
		int bestIdx = 0;
		for (int i = 1; i < scores.length; ++i) {
			if (scores[i] > scores[bestIdx]) {
				bestIdx = i;
			}
		}
		return bestIdx;
	}
	
	protected static double[] normalize(double[] input) {
		double sum = 0.0;
		for (int i = 0; i < input.length; ++i) {
			sum += input[i];
		}
		for (int i = 0; i < input.length; ++i) {
			input[i] /= sum;
		}
		return input;
	}
}
