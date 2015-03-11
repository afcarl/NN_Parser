package deppar;

import java.util.HashMap;
import java.util.ArrayList;
import java.lang.Long;
import java.lang.Integer;

public class Dictionary {
	private HashMap<String, Long> form_num_dic = new HashMap<String, Long>();
	private HashMap<String, Integer> pos_num_dic = new HashMap<String, Integer>();
	private HashMap<String, Integer> deprel_num_dic = new HashMap<String, Integer>();
	
	private HashMap<Long, String> num_form_dic = new HashMap<Long, String>();
	private HashMap<Integer, String> num_pos_dic = new HashMap<Integer, String>();
	private HashMap<Integer, String> num_deprel_dic = new HashMap<Integer, String>();

	private long form_num_dic_size = 0;
	private int pos_num_dic_size = 0;
	private int deprel_num_dic_size = 0;

	public Dictionary(){
		
	}
	
	private void addToDictionary(Word w){
		if(!form_num_dic.containsKey(w.form)){
			form_num_dic.put(w.form, form_num_dic_size);
			num_form_dic.put(form_num_dic_size, w.form);
			form_num_dic_size++;
		}
		
		if(!pos_num_dic.containsKey(w.pos)){
			pos_num_dic.put(w.pos, pos_num_dic_size);
			num_pos_dic.put(pos_num_dic_size, w.pos);
			pos_num_dic_size++;
		}
		if(!deprel_num_dic.containsKey(w.deprel)){
			deprel_num_dic.put(w.deprel, deprel_num_dic_size);
			num_deprel_dic.put(deprel_num_dic_size, w.deprel);
			deprel_num_dic_size++;	
		}
	}
	
	public long getFormSize() {
		return form_num_dic_size;
	}

	public int getPosSize() {
		return pos_num_dic_size;
	}

	public int getDeprelSize() {
		return deprel_num_dic_size;
	}

	public void Construct(ArrayList<Sentence> sens){
		// Adding START and END to the dictionary
		addToDictionary(Word.START());
		addToDictionary(Word.END());
		for (Sentence s : sens){
			for (Word w : s.getWordList()){
				addToDictionary(w);
			}
		}
		form_num_dic.put("[OOV]", form_num_dic_size);
		form_num_dic_size++;
	}

	public long formToNum(String form) {
		if(form_num_dic.containsKey(form)){
			return form_num_dic.get(form);
		}else{
			return form_num_dic.get("[OOV]");
		}
	}

	public int posToNum(String pos) {
		return pos_num_dic.get(pos);
	}

	public int deprelToNum(String deprel) {
		return deprel_num_dic.get(deprel);
	}
	
	public String numToForm(Long num){
		return num_form_dic.get(num);
	}
	
	public String numToPOS(int num){
		return num_pos_dic.get(num);
	}
	
	public String numToDeprel(int num){
		return num_deprel_dic.get(num);
	}
	
}
