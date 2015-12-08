package moa.tasks.openml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.openml.moa.ResultListener;
import org.openml.moa.algorithm.InstancesHelper;
import org.openml.apiconnector.models.Metric;
import org.openml.apiconnector.models.MetricScore;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Config;
import org.openml.apiconnector.xml.Task;

import moa.classifiers.Classifier;
import moa.core.Measurement;
import moa.core.ObjectRepository;
import moa.core.TimingUtils;
import moa.evaluation.ClassificationPerformanceEvaluator;
import moa.evaluation.LearningCurve;
import moa.evaluation.LearningEvaluation;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.IntOption;
import moa.streams.InstanceStream;
import moa.streams.openml.OpenmlTaskReader;
import moa.tasks.MainTask;
import moa.tasks.TaskMonitor;
import weka.core.Instance;

public class OpenmlDataStreamClassification extends MainTask {

	private static final long serialVersionUID = 514834511072776265L;

	@Override
	public String getPurposeString() {
		return "Evaluates a classifier on an OpenML Data Stream Classification Task.";
	}

	public ClassOption learnerOption = new ClassOption("learner", 'l',
			"Classifier to train.", Classifier.class, "bayes.NaiveBayes");

	public IntOption openmlTaskIdOption = new IntOption(
            "taskId",
            't',
            "The OpenML task that will be performed.",
            1, 1, Integer.MAX_VALUE);

	public ClassOption evaluatorOption = new ClassOption("evaluator", 'e',
			"Classification performance evaluation method.",
			ClassificationPerformanceEvaluator.class,
			"BasicClassificationPerformanceEvaluator");

	public IntOption sampleFrequencyOption = new IntOption("sampleFrequency",
			'f',
			"How many instances between samples of the learning performance.",
			100000, 0, Integer.MAX_VALUE);

	public FileOption dumpFileOption = new FileOption("dumpFile", 'd',
			"File to append intermediate csv reslts to.", null, "csv", true);
	
	private InstanceStream stream;
	private ResultListener resultListener;
	private Config config;
	private OpenmlConnector apiconnector;

	@Override
	public Class<?> getTaskResultType() {
		return LearningCurve.class;
	}

	@Override
	protected Object doMainTask(TaskMonitor monitor, ObjectRepository repository) {
		try {
			config = new Config();
		} catch (Exception e) { 
			throw new RuntimeException("Error loading config file openml.conf. Please check whether it exists. " + e.getMessage() );
		}
		
		if( config.getServer() != null ) {
			apiconnector = new OpenmlConnector( config.getServer(), config.getApiKey() );
		} else { 
			apiconnector = new OpenmlConnector( config.getApiKey() );
		} 
		
		String learnerString = this.learnerOption.getValueAsCLIString();
		String streamString = "OpenmlTaskReader -t "+openmlTaskIdOption.getValue();

		Classifier learner = (Classifier) getPreparedClassOption(this.learnerOption);
		stream = new OpenmlTaskReader(apiconnector, openmlTaskIdOption.getValue());
		Task task = ((OpenmlTaskReader)stream).getTask();
		
		try {
			resultListener = new ResultListener(task, apiconnector);
		} catch ( Exception e )  {
			throw new RuntimeException("Error initializing ResultListener. " + e.getMessage() );
		}
		
		ClassificationPerformanceEvaluator evaluator = (ClassificationPerformanceEvaluator) getPreparedClassOption(this.evaluatorOption);
		learner.setModelContext(stream.getHeader());
		long instancesProcessed = 0;
		Conversion.log( "OK", "Download", "Obtained Stream Header. " );

		monitor.setCurrentActivity("Evaluating learner...", -1.0);
		LearningCurve learningCurve = new LearningCurve(
				"learning evaluation instances");
		File dumpFile = this.dumpFileOption.getFile();
		PrintStream immediateResultStream = null;
		if (dumpFile != null) {
			try {
				if (dumpFile.exists()) {
					immediateResultStream = new PrintStream(
							new FileOutputStream(dumpFile, true), true);
				} else {
					immediateResultStream = new PrintStream(
							new FileOutputStream(dumpFile), true);
				}
			} catch (Exception ex) {
				throw new RuntimeException(
						"Unable to open immediate result file: " + dumpFile, ex);
			}
		}
		boolean firstDump = true;
		boolean preciseCPUTiming = TimingUtils.enablePreciseTiming();
		long evaluateStartTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		long lastEvaluateStartTime = evaluateStartTime;
		double RAMHours = 0.0;
		int instanceCounter = 0;
		
		while (stream.hasMoreInstances()) {
			Instance trainInst = stream.nextInstance();
			Instance testInst = (Instance) trainInst.copy();
			// testInst.setClassMissing();
			double[] prediction = learner.getVotesForInstance(testInst);
			// evaluator.addClassificationAttempt(trueClass, prediction,
			// testInst
			// .weight());
			
			try {
				resultListener.addPrediction(instanceCounter++, InstancesHelper.toProbDist( prediction ), (int) trainInst.classValue() );
			} catch (IOException e) {
				throw new RuntimeException("Error adding prediction: " + e.getMessage());
			}
			
			learner.trainOnInstance(trainInst);
			evaluator.addResult(testInst, prediction);
			
			instancesProcessed++;
			if (instancesProcessed % this.sampleFrequencyOption.getValue() == 0 || stream.hasMoreInstances() == false) {
				long evaluateTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
				double time = TimingUtils.nanoTimeToSeconds(evaluateTime - evaluateStartTime);
				double timeIncrement = TimingUtils.nanoTimeToSeconds(evaluateTime - lastEvaluateStartTime);
				double RAMHoursIncrement = learner.measureByteSize() / (1024.0 * 1024.0 * 1024.0); // GBs
				RAMHoursIncrement *= (timeIncrement / 3600.0); // Hours
				RAMHours += RAMHoursIncrement;
				lastEvaluateStartTime = evaluateTime;
				learningCurve.insertEntry(new LearningEvaluation(
						new Measurement[] {
								new Measurement(
										"learning evaluation instances", instancesProcessed),
								new Measurement("evaluation time ("
										+ (preciseCPUTiming ? "cpu " : "")
										+ "seconds)", time),
								new Measurement("model cost (RAM-Hours)", RAMHours) }, evaluator, learner));
				if (immediateResultStream != null) {
					if (firstDump) {
						immediateResultStream.print("Learner,stream,");
						immediateResultStream.println(learningCurve.headerToString());
						firstDump = false;
					}
					immediateResultStream.print(learnerString + "," + streamString + "," );
					immediateResultStream.println(learningCurve.entryToString(learningCurve.numEntries() - 1));
					immediateResultStream.flush();
				}
			}
			if (instancesProcessed % INSTANCES_BETWEEN_MONITOR_UPDATES == 0) {
				if (monitor.taskShouldAbort()) {
					return null;
				}
				long estimatedRemainingInstances = stream.estimatedRemainingInstances();

				monitor.setCurrentActivityFractionComplete(estimatedRemainingInstances < 0 ? -1.0
						: (double) instancesProcessed / (double) (instancesProcessed + estimatedRemainingInstances));
				if (monitor.resultPreviewRequested()) {
					monitor.setLatestResultPreview(learningCurve.copy());
				}
			}
		}
		if (immediateResultStream != null) {
			immediateResultStream.close();
		}
		long evaluateEndTime = TimingUtils.getNanoCPUTimeOfCurrentThread();
		
		Map<String, Double> evaluatorResults = new HashMap<String, Double>();
		for( Measurement m : evaluator.getPerformanceMeasurements() ) {
			evaluatorResults.put( m.getName(), m.getValue() );
		}
		
		Map<Metric, MetricScore> m = new HashMap<Metric, MetricScore>();
		m.put( new Metric("ram_hours", "openml.userdefined.ram_hours(1.0)"), new MetricScore(RAMHours, instancesProcessed) );
		m.put( new Metric("run_cpu_time", "openml.evaluation.run_cpu_time(1.0)"), 
			new MetricScore( TimingUtils.nanoTimeToSeconds(evaluateEndTime - evaluateStartTime), instancesProcessed ) );
		// TODO! Keys "Kappa Statistic (percent)" and "classifications correct (percent)" are dangerous to use in this way. 
		m.put( new Metric("predictive_accuracy", "openml.evaluation.predictive_accuracy(1.0)"), 
			new MetricScore( evaluatorResults.get("classifications correct (percent)") / 100, instancesProcessed ) ); // division 100 for percentages to pred_acc
		m.put( new Metric("kappa", "openml.evaluation.kappa(1.0)"), 
			new MetricScore( ( evaluatorResults.get("Kappa Statistic (percent)") / 100 ), instancesProcessed ) ); // division 100 for percentages to pred_acc

		try { 
			resultListener.sendToOpenML( learner, m ); 
		} catch( Exception e ) {
			throw new RuntimeException("Error uploading result to OpenML: " + e.getMessage() );
		}
		
		return learningCurve;
	}
}
