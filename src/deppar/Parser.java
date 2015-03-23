package deppar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import jnn.features.FeatureVector;
import jnn.functions.nonparametrized.TanSigmoidLayer;
import jnn.functions.parametrized.DenseFullyConnectedLayer;
import jnn.mapping.OutputMappingDenseToDense;
import jnn.neuron.DenseNeuronArray;
import jnn.training.TreeInference;
import AD3.Arc;
import AD3.ParserDecoder;

public class Parser {
	private Dictionary dic;
	public ArrayList<Sentence> corpus;
	private HashMap<String, double[]> wordvecs = new HashMap<String, double[]>();
	private int embedVecLength = 0;
	public ParserDecoder decoder = new ParserDecoder();
	public Parser() {
		corpus = Sentence.readFromFile(new File("output.dev.converted"));
		dic = new Dictionary();
		dic.Construct(corpus);
		for (Sentence sen : corpus) {
			sen.numeralize(dic);
		}
		System.out.println("loading");
		loadWordEmbedding(new File("ptb_embed_vector_readable"));
	}

	private void loadWordEmbedding(File f) {
		try {
			String text = new String(Files.readAllBytes(f.toPath()));
			String[] lines = text.trim().split("\n");
			for (String line : lines) {
				if (line.trim().equals(""))
					continue;
				String[] args = line.trim().split(" ");
				double[] vec = new double[args.length - 1];
				for (int i = 0; i < vec.length; i++) {
					vec[i] = Double.parseDouble(args[i + 1]);
				}
				wordvecs.put(args[0], vec);
				embedVecLength = args.length - 1;
			}
//			double[] vec = new double[embedVecLength];
//			for(int i = 0; i < vec.length; i++){
//				vec[i] = 0;
//			}
//			wordvecs.put("<s>", vec);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getEmbedVecLenght() {
		return embedVecLength;
	}

	public double[] checkOutWordEmbeding(String w) {
		if (wordvecs.containsKey(w)) {
			return wordvecs.get(w);
		} else {
			return wordvecs.get("<UNK>");
		}
	}
	
	public double[] checkOutWordEmbedingBinary(long id) {
		double[] vec = new double[50];
		for(int i = 0; i < vec.length; i++){
			vec[i] = 0;
		}
		int idd = (int)id;
		vec[idd] = 1;
		return vec;
	}

	public static double[] concat(double[] a, double[] b) {
		int aLen = a.length;
		int bLen = b.length;
		double[] c = new double[aLen + bLen];
		System.arraycopy(a, 0, c, 0, aLen);
		System.arraycopy(b, 0, c, aLen, bLen);
		return c;
	}

//	private DenseNeuronArray TokenStructure(Word w) {
//		DenseNeuronArray Token = new DenseNeuronArray(this.getEmbedVecLenght());
//		double[] vec = checkOutWordEmbeding(w.form);
//		Token.init();
//		Token.loadFromArray(vec);
//		return Token;
//	}

	public static void main(String[] args) {
//		double[] d = new double[] { 1.3, 1.5, 2.1 };
//		double[] e = new double[] { 3.2, 4.5, 3.5 };
//		double[] n = concat(d, e);
//		for (double x : n) {
//			System.out.println(x);
//		}
		
		FeatureVector.useMomentumDefault = true;
		FeatureVector.learningRateDefault = 0.1;
		
		Parser parser = new Parser();
		int inputSize = parser.getEmbedVecLenght() * 2 + 2;
		// for debugging purpose only
		// int hiddenLayer1Dim = 200;
		int hiddenLayer1Dim = 1;
		
		int hiddenLayer2Dim = 1;

		DenseFullyConnectedLayer hidden1Parameters = new DenseFullyConnectedLayer(inputSize, hiddenLayer1Dim);
		// debug purpose
		//DenseFullyConnectedLayer hidden2Parameters = new DenseFullyConnectedLayer(hiddenLayer1Dim, hiddenLayer2Dim);
		// FeatureVector.learningRateDefault = 0.000005;
		
		
		HashMap<Arc, TreeInference> inferences = new HashMap<Arc, TreeInference>();
		HashMap<Arc, DenseNeuronArray> outputNeurons = new HashMap<Arc, DenseNeuronArray>();
		
		System.out.print(parser.corpus.size());
		
		HashMap<Arc, Double> last_time_score = new HashMap<Arc, Double>();
		
		for (Sentence s : parser.corpus) {
			s = parser.corpus.get(0);
			ArrayList<Arc> arcs = new ArrayList<Arc>();
			ArrayList<Double> arc_scores = new ArrayList<Double>();
			ArrayList<Double> arc_cost_augmented_scores = new ArrayList<Double>();
			
			HashMap<Arc, Double> score_map = new HashMap<Arc, Double>();
			HashMap<Arc, Double> cost_augmented_score_map = new HashMap<Arc, Double>();
			
			for (int h = 0; h < s.length(); h++) {
				for (int m = 1; m < s.length(); m++) {
					if(h == m) continue;
					
					Arc arc = new Arc(h, m);
					arcs.add(arc);
					
					TreeInference inference = new TreeInference(0);
					DenseNeuronArray input = new DenseNeuronArray(inputSize);
					inference.addNeurons(0, input);
					input.setName("input for arc:" + arc.toString());
					
					
					DenseNeuronArray hidden1 = new DenseNeuronArray(hiddenLayer1Dim);
					hidden1.setName("hidden 1 ");
					inference.addNeurons(1, hidden1);
					inference.addMapping(new OutputMappingDenseToDense(input, hidden1, hidden1Parameters));
					
					// a very simple model test if things work
					
					DenseNeuronArray hidden1tanh = new DenseNeuronArray(hiddenLayer1Dim);
					hidden1tanh.setName("hidden 1 tanh ");
					inference.addNeurons(2, hidden1tanh);
					inference.addMapping(new OutputMappingDenseToDense(hidden1, hidden1tanh, TanSigmoidLayer.singleton));
					DenseNeuronArray output = hidden1tanh;
					// should only be use for debugging purpose
					
					/*
					DenseNeuronArray hidden1tanh = new DenseNeuronArray(hiddenLayer1Dim);
					hidden1tanh.setName("hidden 1 tanh ");
					inference.addNeurons(2, hidden1tanh);
					inference.addMapping(new OutputMappingDenseToDense(hidden1, hidden1tanh, TanSigmoidLayer.singleton));
					DenseNeuronArray hidden2 = new DenseNeuronArray(hiddenLayer2Dim);
					hidden2.setName("hidden 2");
					inference.addNeurons(3, hidden2);
					inference.addMapping(new OutputMappingDenseToDense(hidden1tanh, hidden2, hidden2Parameters));
					
					DenseNeuronArray output = new DenseNeuronArray(1);
					inference.addNeurons(4, output);
					output.setName("output for arc:" + arc.toString());
					
					inference.addMapping(new OutputMappingDenseToDense(hidden2, output, TanSigmoidLayer.singleton));
					*/
					
					
					inferences.put(arc, inference);
					outputNeurons.put(arc, output);
					
					
					
					
					Word head = s.getWordIndexAt(h);
					Word child = s.getWordIndexAt(m);
					
//					System.out.println(head.form + "\t" + head.form_id);
//					System.out.println(child.form + "\t" + child.form_id);
					
					double[] dir_length = new double[2];
					dir_length[0] = 0; //(h - m) > 0 ? 1: 0;
				    dir_length[1] = 0; // Math.abs(h-m);

					input.init();
					//input.loadFromArray(concat(concat(parser.checkOutWordEmbeding(head.form), parser.checkOutWordEmbeding(child.form)), dir_length));
					input.loadFromArray(concat(concat(parser.checkOutWordEmbedingBinary(head.form_id), parser.checkOutWordEmbedingBinary(child.form_id)), dir_length));
					inference.init();
					inference.forward();
					
					double arc_score = output.getNeuron(0);
					double arc_cost_augmented_score = arc_score;
					if (!s.goldArcs.contains(arc)){
						arc_cost_augmented_score += 0.1;
					}
					
					arc_scores.add(arc_score);
					arc_cost_augmented_scores.add(arc_cost_augmented_score);
					
					score_map.put(arc, arc_score);
					cost_augmented_score_map.put(arc, arc_cost_augmented_score);
//					inferences.put(arc, inference);
//					outputNeurons.put(arc, output);
				}
			}
			
			ArrayList<Double> posteriors = new ArrayList<Double>();
			for(int i = 0; i < arcs.size();i++){
				posteriors.add(0.0);
			}
			ArrayList<Integer> heads = new ArrayList<Integer>();
			for(int i = 0; i < s.length(); i++){
				heads.add(-1);
			}
//			for(int i = 0; i < arc_scores.size(); i++){
//				System.out.println(arc_scores.get(i));
//			}
			System.out.println("decoding");
			parser.decoder.DecodeAD3Basic(arcs, arc_cost_augmented_scores, s.length(), posteriors, heads);
			System.out.println("finish decoding");
//			for(int i = 0; i < posteriors.size(); i++){
//				System.out.println(posteriors.get(i));
//			}
			for(int i = 0; i < heads.size(); i++){
			System.out.println(heads.get(i));
			}
			double total = 0;
			double error = 0;
			HashSet<Arc> predicted_arcs = new HashSet<Arc>();
			for (int i = 0; i < arcs.size(); i++){
				if(posteriors.get(i) > 0.9){
					predicted_arcs.add(arcs.get(i));
				}
			}
			for(Arc a : predicted_arcs){
				error += cost_augmented_score_map.get(a);
			}
			for (Arc a : s.goldArcs){
				error -= score_map.get(a);
			}
			for (Arc a : predicted_arcs){
				if(s.goldArcs.contains(a)) {
					outputNeurons.get(a).addError(0, 1 - score_map.get(a));
					inferences.get(a).backward();
				}else{
					outputNeurons.get(a).addError(0, -1 - score_map.get(a));
					inferences.get(a).backward();
				}
			}
			
			/* for debugging purpose
//			if (error > 0){
				for (Arc a : predicted_arcs){
					if(s.goldArcs.contains(a)) {
						continue;
					}
					else{
						//System.out.print("kkkkkkk" + score_map.get(a) + "\t" + outputNeurons.get(a).getNeuron(0));
						outputNeurons.get(a).addError(0, -1 - score_map.get(a));
						System.out.println("hhhhhh " + outputNeurons.get(a) + " hhhhh " + score_map.get(a));
						
						System.out.println("beforebefore");
						System.out.println(outputNeurons.get(a));
						System.out.println("beforebefore");
						
						inferences.get(a).backward();
						
						//System.out.println("afterafter");
						//inferences.get(a).printNeurons();
						//System.out.println("afterafter");
//						
//						inferences.get(a).commit(0.1);
//						inferences.get(a).init();
//						inferences.get(a).forward();
//						
//						System.out.println("after+after");
//						System.out.println(outputNeurons.get(a));
//						System.out.println("after+after");
					}
				}
				for (Arc a : s.goldArcs){
					if (predicted_arcs.contains(a)){
						continue;
					}else{
						//System.out.print("kkkkkkk" + score_map.get(a) + "\t" + outputNeurons.get(a).getNeuron(0));
						outputNeurons.get(a).addError(0, 1 - score_map.get(a));
						//System.out.println("hhhhhh" + outputNeurons.get(a));
						
//						System.out.println("beforebefore");
//						System.out.println(outputNeurons.get(a));
//						System.out.println("beforebefore");
						
						inferences.get(a).backward();
						
						//System.out.println("afterafter");
						//inferences.get(a).printNeurons();
						//System.out.println("afterafter");
						
//						inferences.get(a).commit(0.1);
//						inferences.get(a).init();
//						inferences.get(a).forward();
//						
//						System.out.println("after+after");
//						System.out.println(outputNeurons.get(a));
//						System.out.println("after+after");
					}
				}
				*/
				hidden1Parameters.updateWeights(0, 0);
				// debug purpose
				//hidden2Parameters.updateWeights(0, 0);
				
//				System.out.println("++++++");
//				System.out.println(hidden1Parameters.toString());
//				System.out.println(hidden2Parameters.toString());
//				System.out.println("++++++");
				
				
				
				
				// if(Math.abs(error) < 1e-14) continue;
//				total += Math.abs(error);
//				output.addError(0, error);
//				inference.backward();
				
				
//			}else{
//				System.err.println("error < 0: \t" + error);
//			}
			System.err.println("=====");
			for (Arc arc : predicted_arcs){
				System.err.println(arc.toString() + "\t" + score_map.get(arc) + "\t" + last_time_score.get(arc));
				
			}
			System.err.println("=====");
			for (Arc arc : s.goldArcs){
				System.err.println(arc.toString() + "\t" + score_map.get(arc) + "\t" + last_time_score.get(arc));
				
			}
			
			last_time_score = new HashMap<Arc, Double>(score_map);
			
			System.err.println("=====");
			
			
			predicted_arcs.retainAll(s.goldArcs);
			System.err.println((double) predicted_arcs.size() / s.goldArcs.size());
			//System.err.println("*" + total);
		}

	}

}
