package deppar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;

import AD3.Arc;

public class Sentence {
	public ArrayList<Word> words = new ArrayList<Word>();
	public HashSet<Arc> goldArcs = new HashSet<Arc>();

	public Sentence(String conllStr) {
		String[] strs = conllStr.split("\n");
		words = new ArrayList<Word>();
		for (String line : strs) {
			if (line.trim().equals("")) continue;
			Word w = new Word(line);
			words.add(w);
		}
		
		for(Word word : words){
			
			goldArcs.add(new Arc(word.head, word.id));
		}
	}
	// Indexing System Start from 1, root is 0
	public Word getWordIndexAt(int i) {
		if (i == 0) {
			return Word.START();
		} else if (i > words.size() + 1) {
			return Word.END();
		} else {
			return words.get(i-1);
		}
	}

	public ArrayList<Word> getWordList() {
		return words;
	}
	
	public int length(){
		return words.size()+1;
	}
	
	public String toConllString(){
		String s = "";
		for(Word w : words){
			s = s + w.toConllLine() + "\n";
		}
		//s = s + "\n";
		return s;
	}

	public static ArrayList<Sentence> readFromFile(File f) {
		try {
			String text = new String(Files.readAllBytes(f.toPath()));
			String[] sens = text.split("\n\n");
			ArrayList<Sentence> corpus = new ArrayList<Sentence>();

			for (String s : sens) {
				if (s.trim().equals(""))
					continue;
				corpus.add(new Sentence(s));
			}
//			for(Sentence s : corpus){
//				System.out.println(s.toConllString());
//			}
//			System.out.println(corpus.size());
			return corpus;
			
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void numeralize(Dictionary dic){
		for (Word w : words){
			w.numeralize(dic);
		}
	}

	public static void main(String[] args) {
		ArrayList<Sentence> corpus = readFromFile(new File("output.dev.converted"));
		Dictionary dic = new Dictionary();
		dic.Construct(corpus);
		for(Sentence sen : corpus){
			sen.numeralize(dic);
		}
		
		Sentence s = corpus.get(110);
		System.out.println(s.words.get(3).form);
		System.out.println(dic.formToNum(s.words.get(3).form));
		System.out.println(dic.numToForm(dic.formToNum(s.words.get(3).form)));

	}
}
