package AD3;

import java.util.ArrayList;
import java.util.Random;

public class ParserDecoder {
	static {
		System.loadLibrary("ad3parser"); // Load native library at runtime
		// ad3parser.dll (Windows) or libad3parser.so (Unixes)
	}

	// Decode the parsing result using basic model (arc-factored)
	public native void DecodeAD3Basic(ArrayList<Arc> arcs,
			ArrayList<Double> arc_scores, int sentence_length,
			ArrayList<Double> posteriors, ArrayList<Integer> heads);
	
	public static void main(String[] args) {
		ArrayList<Arc> arcs = new ArrayList<Arc>();
		int sentence_length = 10;
		for (int m = 1; m < sentence_length; ++m) {
			for (int h = 0; h < sentence_length; ++h) {
				if (h == m)
					continue;
				Arc arc = new Arc(h, m);
				arcs.add(arc);
			}
		}
		Random r=new Random();
		ArrayList<Double> arc_scores = new ArrayList<Double>();
		for(int i = 0; i < arcs.size(); i++){
			arc_scores.add(r.nextDouble());
		}
		ArrayList<Double> posteriors = new ArrayList<Double>();
		for(int i = 0; i < arcs.size();i++){
			posteriors.add(0.0);
		}
		ArrayList<Integer> heads = new ArrayList<Integer>();
		for(int i = 0; i < sentence_length; i++){
			heads.add(-1);
		}
		
		new ParserDecoder().DecodeAD3Basic(arcs, arc_scores, sentence_length, posteriors, heads);
		
		for(int i = 0; i < arcs.size();i++){
			System.out.println(posteriors.get(i));
		}
		System.out.println("****");
		for(int i = 0; i < sentence_length; i++){
			System.out.println(heads.get(i));
		}
	}

}
