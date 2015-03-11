#include "ad3/FactorGraph.h"

#include "FactorTree.h"
#include <cstdlib>
#include "AD3_ParserDecoder.h"

using namespace AD3;

// Recover a valid parse tree from a possibly fractional solution.
// This is done as described in
//
// André F. T. Martins, Noah A. Smith, and Eric P. Xing.
// "Concise Integer Linear Programming Formulations for Dependency Parsing."
//  Annual Meeting of the Association for Computational Linguistics, 2009.

// Basically we use the fractional memberships as scores and invoke
// the Chu-Liu-Edmonds algorithm.

void GetBestParse(int sentence_length,
                  const vector<Arc*> &arcs,
                  const vector<double> &arc_scores,
                  Factor *tree_factor, const vector<double> &posteriors, vector<int> &heads) {
  vector<double> scores(arcs.size());
  for (int i = 0; i < arcs.size(); ++i) {
    scores[i] = posteriors[i];
  }

  double best_value;
  static_cast<FactorTree*>(tree_factor)->RunCLE(scores, &heads, &best_value);
  cout << best_value << endl;
}

void DecodeAD3Basic(vector<Arc*> &arcs, vector<double> arc_scores, int sentence_length, vector<double> &posteriors, vector<int> &heads){
 
  FactorGraph factor_graph;
  vector<Factor*> factors;
  // Create variables (one per arc).
  vector<BinaryVariable*> variables(arcs.size());
  for (int i = 0; i < arcs.size(); ++i) {
    BinaryVariable* variable = factor_graph.CreateBinaryVariable();
    variable->SetLogPotential(arc_scores[i]);
    variables[i] = variable;
  }

  cout << "Creating tree factor..."
       << endl;

  Factor *tree_factor = new FactorTree;
  factors.push_back(tree_factor);
  factor_graph.DeclareFactor(tree_factor,
                             variables);
  static_cast<FactorTree*>(tree_factor)->Initialize(sentence_length, arcs);

  vector<double> additional_posteriors;
  double value;

  // Run AD3.
  cout << "Running AD3..."
       << endl;
  factor_graph.SetEtaAD3(0.1);
  factor_graph.AdaptEtaAD3(true);
  factor_graph.SetMaxIterationsAD3(1000);
  factor_graph.SolveLPMAPWithAD3(&posteriors, &additional_posteriors, &value);

  GetBestParse(sentence_length, arcs, arc_scores, tree_factor, posteriors, heads);
  
  // Out put the posteriors. For debugging purpose.
  // cout << "Posteriors: " << endl;
  // for (int i = 0; i < posteriors.size(); ++i) {
  //   cout << posteriors[i] << " ";

  // }
  // cout << endl;

}

JNIEXPORT void JNICALL Java_AD3_ParserDecoder_DecodeAD3Basic
  (JNIEnv *env, jobject thisObj, jobject j_arcs, jobject j_scores, jint sentence_length, jobject j_posteriors, jobject j_heads){
    jclass c_arraylist = env->FindClass("java/util/ArrayList");
    jmethodID fset_id = env->GetMethodID(c_arraylist,"set","(ILjava/lang/Object;)Ljava/lang/Object;");
    jmethodID fget_id = env->GetMethodID(c_arraylist,"get","(I)Ljava/lang/Object;");
    jmethodID fsize_id = env->GetMethodID(c_arraylist,"size","()I");

    jclass c_double = env->FindClass("java/lang/Double");
    jmethodID fdoublevalue_id = env->GetMethodID(c_double,"doubleValue","()D");
    jmethodID fdouble_init = env->GetMethodID(c_double, "<init>","(D)V");

    jclass c_integer = env->FindClass("java/lang/Integer");
    jmethodID f_intvalue_id = env->GetMethodID(c_integer,"intValue","()I");
    jmethodID f_int_init = env->GetMethodID(c_integer, "<init>", "(I)V");

    jclass c_arc = env->FindClass("AD3/Arc");
    jmethodID farc_head = env->GetMethodID(c_arc, "head", "()I");
    jmethodID farc_modifier = env->GetMethodID(c_arc, "modifier", "()I");

    // Constructing the arcs
    vector<Arc*> arcs;
    int arc_size = env->CallIntMethod(j_arcs, fsize_id);

    for (int i = 0; i < arc_size; ++i){
      jobject this_arc = env->CallObjectMethod(j_arcs, fget_id, i);
      int h = env->CallIntMethod(this_arc, farc_head);
      int m = env->CallIntMethod(this_arc, farc_modifier);
      Arc *arc = new Arc(h, m);
      arcs.push_back(arc);
    }

    // Constructing the scores
    vector<double> arc_scores;
    for (int i = 0; i < arc_size; ++i){
      jobject this_score = env->CallObjectMethod(j_scores, fget_id, i);
      double v = env->CallDoubleMethod(this_score, fdoublevalue_id);
      arc_scores.push_back(v);
    }

    // sentence length does not need any more operations
    for(int i = 0; i < arc_size; ++i){
      printf("%lf\n", arc_scores[i]);
    }

    // Start decoding
    vector<int> heads(sentence_length);
    vector<double> posteriors;
    DecodeAD3Basic(arcs, arc_scores, sentence_length, posteriors, heads);

    // Passing the values back
    for (int i = 0; i < arc_size; ++i){
      jobject value =  env->NewObject(c_double, fdouble_init, posteriors[i]);
      env->CallObjectMethod(j_posteriors, fset_id, i, value);
    }

    for (int i = 0; i < sentence_length; ++i){
      jobject value =  env->NewObject(c_integer, f_int_init, heads[i]);
      env->CallObjectMethod(j_heads, fset_id, i, value);
    }
}



int main(int argc, char **argv) {

  int sentence_length = 10;

  vector<Arc*> arcs;
  for (int m = 1; m < sentence_length; ++m) {
    for (int h = 0; h < sentence_length; ++h) {
      if (h == m) continue;
      Arc *arc = new Arc(h, m);
      arcs.push_back(arc);
    }
  }

  vector<double> arc_scores(arcs.size());
  srand((unsigned)time(NULL));
  // For test purpose, random assign the scores to arcs.
  for (int i = 0; i < arcs.size(); ++i) {
    double score = static_cast<double>(rand()) /
      static_cast<double>(RAND_MAX) - 0.5;
    arc_scores[i] = (double) score;
  }

  vector<int> heads(sentence_length);
  vector<double> posteriors;
  DecodeAD3Basic(arcs, arc_scores, sentence_length, posteriors, heads);

  for (int i = 0; i < heads.size(); ++i) {
    cout << heads[i] << " ";
  }
  cout << endl;

  for (int i = 0; i < posteriors.size(); ++i) {
    cout << posteriors[i] << " ";

  }


  return 0;
}


