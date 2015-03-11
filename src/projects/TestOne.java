package projects;

import jnn.features.FeatureVector;
import jnn.functions.nonparametrized.TanSigmoidLayer;
import jnn.functions.parametrized.DenseFullyConnectedLayer;
import jnn.mapping.OutputMappingDenseToDense;
import jnn.neuron.DenseNeuronArray;
import jnn.training.TreeInference;
import util.RandomUtils;

public class TestOne {

	public static void main(String[] args){
		int inputSize = 200;
		int samples = 10;
		int hiddenLayer1Dim = 50;
		int hiddenLayer2Dim = 100;
		FeatureVector.useMomentumDefault = true;
		double[][] input = new double[samples][inputSize];
		double[][] expected = new double[samples][hiddenLayer2Dim];
		for(int s = 0; s < samples; s++){
			RandomUtils.initializeRandomArray(input[s], -1, 1, 10);
			RandomUtils.initializeRandomArray(expected[s], -1, 1, 10);
		}
		//PrintUtils.printDoubleMatrix("inputs", input, false);

		DenseFullyConnectedLayer hidden1Parameters = new DenseFullyConnectedLayer(inputSize, hiddenLayer1Dim);
		DenseFullyConnectedLayer hidden2Parameters = new DenseFullyConnectedLayer(hiddenLayer1Dim, hiddenLayer2Dim);

		for(int e = 0; e < 1000; e++){
			for(int s = 0; s < samples; s++){
				//System.err.println("sample " + s);
				double[] inputSample = input[s];
				TreeInference inference = new TreeInference(0);

				DenseNeuronArray inputI = new DenseNeuronArray(inputSize);
				inputI.init();
				inputI.loadFromArray(inputSample);
				inputI.setName("input " + s);
				inference.addNeurons(0, inputI);

				DenseNeuronArray hidden1 = new DenseNeuronArray(hiddenLayer1Dim);
				hidden1.setName("hidden 1 " + s);
				inference.addNeurons(1, hidden1);

				inference.addMapping(new OutputMappingDenseToDense(inputI, hidden1, hidden1Parameters));

				DenseNeuronArray hidden1tanh = new DenseNeuronArray(hiddenLayer1Dim);
				hidden1tanh.setName("hidden 1 tanh " + s);
				inference.addNeurons(2, hidden1tanh);
				
				inference.addMapping(new OutputMappingDenseToDense(hidden1, hidden1tanh, TanSigmoidLayer.singleton));				
				
				DenseNeuronArray hidden2 = new DenseNeuronArray(hiddenLayer2Dim);
				hidden2.setName("hidden 2" + s);
				inference.addNeurons(3, hidden2);

				inference.addMapping(new OutputMappingDenseToDense(hidden1tanh, hidden2, hidden2Parameters));

				inference.init();
				inference.forward();

				double total = 0;
				for(int o = 0; o < hiddenLayer2Dim; o++){
					double error = expected[s][o] - hidden2.getNeuron(o);
					total += Math.abs(error);
					hidden2.addError(o, error);
				}

				inference.backward();			
//				inference.printNeurons();
				System.err.println("*" + total);
				
			}
			hidden1Parameters.updateWeights(0,0);
			hidden2Parameters.updateWeights(0,0);

		}
	}
}
