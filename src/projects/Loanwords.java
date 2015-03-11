package projects;

import java.util.LinkedList;

import jnn.features.FeatureVector;
import jnn.functions.composite.SequentialLSTM;
import jnn.functions.parametrized.SparseFullyConnectedLayer;
import jnn.mapping.OutputMappingDenseArrayToDenseArray;
import jnn.mapping.OutputMappingSparseToDense;
import jnn.neuron.DenseNeuronArray;
import jnn.neuron.SparseNeuronArray;
import jnn.objective.WordSoftmaxDenseObjective;
import jnn.training.TreeInference;
import util.IOUtils;
import vocab.VocabWithHuffmanTree;

public class Loanwords {
	public static final String SOS ="<s>";
	public static final String EOS ="</s>";
	static double error = 0;

	public static void main(String[] args){
		FeatureVector.useMomentumDefault = true;
		FeatureVector.learningRateDefault = 0.01;
		
		String trainFile = "/Users/lingwang/Documents/workspace/ContinuousVectors/yulia-loanwords/data/ro/train.ro-en-fr";
		String frenchWordsFile = "/Users/lingwang/Documents/workspace/ContinuousVectors/yulia-loanwords/data/ro/global_phone.pron-dict.fr";
		int letterProjectionDim = 20;
		int stateDim = 50;

		VocabWithHuffmanTree inputVocab = new VocabWithHuffmanTree();
		VocabWithHuffmanTree outputVocab = new VocabWithHuffmanTree();

		VocabWithHuffmanTree frenchVocab = new VocabWithHuffmanTree();
		IOUtils.iterateFiles(frenchWordsFile, new IOUtils.iterateFilesCallback(){
			@Override
			public void cb(String[] lines, int lineNumber) {
				frenchVocab.addWordToVocab(lines[0].split("\\s+\\|\\|\\|\\s+")[0]);
			}
		});
		
		IOUtils.iterateFiles(trainFile, new IOUtils.iterateFilesCallback(){
			@Override
			public void cb(String[] lines, int lineNumber) {
				String[] cells = lines[0].split("\\s+\\|\\|\\|\\s+");
				String inputWord = cells[1];
				String outputWord = cells[2];
				for(int i = 0 ; i < inputWord.length(); i++){
					String c = String.valueOf(inputWord.charAt(i));
					inputVocab.addWordToVocab(c);
				}
				for(int i = 0 ; i < outputWord.length(); i++){
					String c = String.valueOf(outputWord.charAt(i));
					outputVocab.addWordToVocab(c);
				}				
			}
		});

		inputVocab.addWordToVocab(SOS);
		inputVocab.addWordToVocab(EOS);
		outputVocab.addWordToVocab(SOS);
		outputVocab.addWordToVocab(EOS);
		inputVocab.sortVocabByCount();
		outputVocab.sortVocabByCount();
		inputVocab.generateHuffmanCodes();
		outputVocab.generateHuffmanCodes();

		frenchVocab.sortVocabByCount();
		frenchVocab.generateHuffmanCodes();
		
		System.err.println("input has " + inputVocab.getTypes() + " different characters ");
		System.err.println("output has " + outputVocab.getTypes() + " different characters ");
		System.err.println("french words have " + frenchVocab.getTypes() + " different characters ");

		
		SparseFullyConnectedLayer inputProjectionLayer = new SparseFullyConnectedLayer(inputVocab.getTypes(), letterProjectionDim);
		SparseFullyConnectedLayer outputProjectionLayer = new SparseFullyConnectedLayer(outputVocab.getTypes(), letterProjectionDim);
		SequentialLSTM lstm = new SequentialLSTM(letterProjectionDim, stateDim, outputVocab.getTypes());		
		lstm.outputSigmoid = 0;

		for(int epoch = 0; epoch < 1000; epoch++){
			error = 0;
			IOUtils.iterateFiles(trainFile, new IOUtils.iterateFilesCallback(){
				@Override
				public void cb(String[] lines, int lineNumber) {
					TreeInference inference = new TreeInference(0);
					String[] cells = lines[0].split("\\s+\\|\\|\\|\\s+");
					String inputWord = cells[1];
					String outputWord = cells[2];
					LinkedList<DenseNeuronArray> inputNeurons = new LinkedList<DenseNeuronArray>();
					LinkedList<DenseNeuronArray> outputNeurons = new LinkedList<DenseNeuronArray>();
					LinkedList<WordSoftmaxDenseObjective> objectives = new LinkedList<WordSoftmaxDenseObjective>();

					for(int i = -1 ; i < inputWord.length()+1; i++){
						String c = "";
						if(i == inputWord.length()){
							c = EOS;
						}
						else if(i == -1){
							c = SOS;
						}
						else{
							c = String.valueOf(inputWord.charAt(i));						
						}
						SparseNeuronArray neuron = new SparseNeuronArray(inputVocab.getTypes());
						neuron.addNeuron(inputVocab.getEntry(c).id, 1);
						neuron.setName("input sparse neuron for letter " + c);
						inference.addNeurons(0, neuron);

						DenseNeuronArray denseNeuron = new DenseNeuronArray(letterProjectionDim);
						denseNeuron.setName("input dense neuron for letter " + c);
						inference.addNeurons(1, denseNeuron);

						inference.addMapping(new OutputMappingSparseToDense(neuron, denseNeuron, inputProjectionLayer));
						inputNeurons.addLast(denseNeuron);
					}				
					for(int i = -1 ; i < outputWord.length()+1; i++){
						String c = "";
						if(i == outputWord.length()){
							c = EOS;
						}
						else if(i == -1){
							c = SOS;
						}
						else{
							c = String.valueOf(outputWord.charAt(i));						
						}
						SparseNeuronArray neuron = new SparseNeuronArray(outputVocab.getTypes());
						neuron.addNeuron(outputVocab.getEntry(c).id, 1);
						neuron.setName("output sparse neuron for letter " + c);
						inference.addNeurons(0, neuron);

						DenseNeuronArray denseNeuron = new DenseNeuronArray(letterProjectionDim);
						denseNeuron.setName("output dense neuron for letter " + c);
						inference.addNeurons(1, denseNeuron);

						inference.addMapping(new OutputMappingSparseToDense(neuron, denseNeuron, outputProjectionLayer));
						inputNeurons.addLast(denseNeuron);

						DenseNeuronArray outputDenseNeurons = new DenseNeuronArray(outputVocab.getTypes());
						outputDenseNeurons.setName("output letter for " + i);
						inference.addNeurons(2, outputDenseNeurons);

						outputNeurons.addLast(outputDenseNeurons);
						objectives.addLast(new WordSoftmaxDenseObjective(outputDenseNeurons, outputVocab.getEntry(c).id));					
					}

					DenseNeuronArray[] inputs = inputNeurons.toArray(new DenseNeuronArray[0]);				
					DenseNeuronArray[] outputs = outputNeurons.toArray(new DenseNeuronArray[0]);
					inference.addMapping(new OutputMappingDenseArrayToDenseArray(inputs, outputs, lstm));

					inference.init();
					inference.forward();
					for(WordSoftmaxDenseObjective objective : objectives){
						objective.addError(1/(double)objectives.size());
						error+=objective.getError();
						if(Double.isNaN(error)){
							System.err.println(lineNumber);
							inference.printNeurons();
							System.err.println(lstm);
							System.exit(0);
						}
					}
					inference.backward();
					inference.commit(0.1);
				}
			});
			System.err.println("error was :" + error);
		}
	}


}
