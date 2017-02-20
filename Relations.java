/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
//package stanfordDep;

/**
 *
 * @author s1065915
 */

import edu.stanford.nlp.ling.CoreLabel;
import java.io.*;
import edu.stanford.nlp.trees.*;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.ling.Sentence;

public class Relations {
    //attention: prep labels will be prep_in, prep_for, prep_in_front_of
    //should add prep and allow PREPOSITIONAL_OBJECT in the list of vaules -> this results in pobj that is then collapse to a prep_
    private static final String [] m_relations = new String []{"nsubj","nsubjpass","dobj","iobj"};//"prep" //maybe instead of string mattching I should call just the GrammaticalRelation tregex
    private static final HashMap <String, Boolean> m_relationsHM = InitializeRelationHM(m_relations);
    
    /** Factories for building the parse tree and grammatical structure -> should be instantiated once **/
   /* private static final GrammaticalStructureFactory gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
    private static final TreeFactory tf = new LabeledScoredTreeFactory();
  */
  
    public GrammaticalStructureFactory gsf;
    public TreeFactory tf;
  
   public String specifiedRelations;
   public LexicalizedParser lp;
    
    public Relations(){
        this.specifiedRelations = " ";
        this.gsf = new PennTreebankLanguagePack().grammaticalStructureFactory();
        this.tf = new LabeledScoredTreeFactory();
        if(this.gsf == null || this.tf==null){
          System.err.println("(Relations) IN CONSTRUCTOR: NULL");
          System.err.flush();
        }
      
        //call this outside of constructor
        //this.lp  = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",  "-maxLength", "100", "-retainTmpSubcategories");
       /* if(this.lp == null){
          System.err.println("(Relations) IN CONSTRUCTOR: parser NULL");
          System.err.flush();
        }
        */
        
    }
  //call this from nbest score not from constructor -> otherwise the HeadFeature will initialize this for eveyr hypothesis
  public synchronized void InitLP(){
    this.lp  = LexicalizedParser.loadModel("edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz",  "-maxLength", "100", "-retainTmpSubcategories");
     if(this.lp == null){
     System.err.println("(Relations) IN InitLP: parser NULL");
     System.err.flush();
     }
  }
    
    public synchronized String[] GetRelationList(){
        return m_relations;
    }
    
    /** INITIALIZING ALL STRUCTURES **/
    public static HashMap <String, Boolean> InitializeRelationHM(String [] relations){
        HashMap <String, Boolean> relationsHM = new  HashMap();
        for (String rel: relations){
            relationsHM.put(rel, Boolean.TRUE);
        }
        return relationsHM;
    }
    
    
    public synchronized Tree InitializeTree(String treeString) throws IOException{
        Reader r = new BufferedReader(new StringReader(treeString));
        if(this.tf==null){
          System.err.println("(Relations) NULL TreeFactory ");
          System.err.flush();
          return null;
        }
        TreeReader tr = new PennTreeReader(r, this.tf);
        Tree tree =  tr.readTree();
        return tree;
    }
    
    /** Extract all relations **/
    public synchronized List<TypedDependency> GetAllRelations(Tree tree)throws IOException{
      List<TypedDependency> tdl;
      GrammaticalStructure gs;
   /*   System.err.println("==============TREE==================");
      tree.pennPrint();
      System.err.println();
    */
      if(this.gsf==null){
        System.err.println("(Relations) NULL grammaticalStructureFactory");
        System.err.flush();
        return null;
      }
      
      if(tree==null){
          System.err.println("(Relations) NULL tree in GetAllRelations");
          System.err.flush();
          return null;
      }
      //!! THIS IS WHERE IT CRASHES BUT MIGHT BE SOME PREVIOUS ERROR//
      synchronized(this.gsf){
      gs = this.gsf.newGrammaticalStructure(tree);
      }
      
      if(gs==null){
          System.err.println("NULL GrammaticalStructure");
          System.err.flush();
        return null;
      }
      
      tdl = gs.typedDependenciesCCprocessed(true);
      if(tdl==null){
        System.err.println("(Relations) NULL typedDependenciesCCprocessed");
        System.err.flush();
      }
      
      return tdl;
    }
    
    public String GetAllRelationsAsString(List<TypedDependency> tdl){
        String allRelations = " ";
        for (TypedDependency dep : tdl){
               // allRelations = allRelations + " " + dep.reln()+" "+ dep.gov()+" "+ dep.dep()+"\n";
            allRelations = allRelations + " " + dep.reln() +" "+ dep.gov().toString().replace('-', ' ')+" "+dep.gov().label().tag()+" "+ dep.dep().toString().replace('-', ' ')+" "+dep.dep().label().tag()+" ";
            }
        return allRelations;
    }
    
    public String GetSpecifiedRelationsAsString (List<TypedDependency> tdl)throws IOException{
         return GetSpecifiedRelationsAsString(this.m_relationsHM,tdl);
     }
    
    //Do I need to add synchronization here or does the Object in C++ is synchronized?
    public String GetSpecifiedRelationsAsString(HashMap <String, Boolean> relationsHM, List<TypedDependency> tdl) throws IOException{
        String specifiedRelations = "";
        for (TypedDependency dep : tdl){
            String rel = dep.reln().toString();
            if(rel.startsWith("prep"))
                rel = "prep";
            if(relationsHM.containsKey(rel)){
                specifiedRelations = specifiedRelations + " " + dep.reln() +" "+ dep.gov().toString().replaceFirst("-[^ ]*", " ")+ dep.dep().toString().replaceFirst("-(.*)", "\t");
            }
        }
      return specifiedRelations;
    }
    
    public synchronized String  ProcessParsedSentence(String treeString, boolean specified) throws IOException{
      //should ckeck problems with treeString and specified
      Tree tree = InitializeTree(treeString);
      if(tree==null){
        System.err.println("(Relations) NULL Tree");
	System.err.println(treeString);
        System.err.flush();
        return "EMPTY";
      }
      
      List<TypedDependency> tdl;
      tdl = GetAllRelations(tree);
      if(tdl==null){
        System.err.println("(Relations) NULL TypedDependencyList");
        return "EMPTY";
      }
      
      if(specified)
          return GetSpecifiedRelationsAsString(tdl);
      else
          return GetAllRelationsAsString(tdl);
     
    }
    
    public synchronized String ProcessSentence(String sent, boolean dummy) throws IOException{
         List<CoreLabel> rawWords = Sentence.toCoreLabelList(StringUtils.split(sent, " "));
         Tree parse = lp.apply(rawWords);
         if(parse==null){
            System.err.println("(Relations) NULL parse");
            System.err.println(sent);
            System.err.flush();
            return "EMPTY";
          }
        List<TypedDependency> tdl;
        GrammaticalStructure gs;
        synchronized(this.gsf){
          gs = this.gsf.newGrammaticalStructure(parse);
        }
        tdl = gs.typedDependenciesCCprocessed();
        String allRelations = "";
        for (TypedDependency dep : tdl){
               // allRelations = allRelations + " " + dep.reln()+" "+ dep.gov()+" "+ dep.dep()+"\n";
            allRelations = allRelations + dep.dep().toString().replaceFirst("(.*)-", "")+ " " + dep.gov().toString().replaceFirst("[^ ]*-", "")+ " "+ dep.reln() +" ";
            }
        if(allRelations=="")
            return "EMPTY";
        return allRelations;
  
    }
    

            
    
    
    public static void main(String[] args) throws IOException{
 
        String testSentence2 =  "(S (NP (PRP$ My) (NN dog) (CC and) (PRP$ My) (NN cat)) (ADVP (RB also)) (VP (VBZ like) (S (VP (VBG eating) (NP (NN sausage)))))))";
        String testSentence3 = "((S (NP (NNP Sam)) (VP (VBD died) (NP-TMP (NN today)))))";
        String testSentence4 = "(SBARQ (WHNP (WP Who)) (SQ (VBZ is) (NP (DT the) (NN president))) (. ?))";
        String testSentence5 = "(S (NP (PRP I)) (VP (VBD took) (NP (DT the) (NN book)) (PP (IN from) (NP (PRP you)))) (. .))";
        
         Boolean test_basic = false;
         String testSentence1 = "(SBARQ (WHNP (WDT Which) (JJ Brazilian) (NN game)) (SQ (VBD did) (NP (PRP you)) (VP (VB see))) (. ?))";
        String testSentence6 = "(VP (VB give)(PP (DT a)(JJ separate)(NNP GC)(NN exam)))";
         Relations rel = new Relations();
                      
         
      if(args.length==1 && args[0].compareTo("2")==0){// && args[0]=="2")
        rel.InitLP();
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); //new FileReader("/afs/inf.ed.ac.uk/user/s10/s1065915/Documents/JAVA/src/stanfordDep/test_parsing"))) {
             for(String line; (line = br.readLine()) != null ; ) {
              System.out.println(rel.ProcessSentence(line,true));
            }
           }
      if(args.length==1 && args[0].compareTo("1")==0){
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in)); //new FileReader("/afs/inf.ed.ac.uk/user/s10/s1065915/Documents/JAVA/src/stanfordDep/test_parsing"))) {
                for(String line; (line = br.readLine()) != null ; ) {
                 System.out.print(rel.ProcessParsedSentence(line, true));
               }
            }
        if(args.length==0){
            System.err.println("arguments: 1 for ProcessParsedSentence, 2 for ProcessSentence");
            if(test_basic){
                System.err.println("------------- CCprocessed dependencies - specified --------");
                System.err.println(StringUtils.join(rel.GetRelationList(), " "));
                //All relations
                //System.err.println(rel.ProcessParsedSentence(testSentence5, false));
                //Specified relations
                System.err.println(rel.ProcessParsedSentence(testSentence1, true));
             }
        }
        
         
     /*
        if (test_all) {// print the grammatical structure, the basic, collapsed and
        // CCprocessed

        System.err.println("============= parse tree =======================");
        tree.pennPrint();
        System.err.println();

        System.err.println("------------- GrammaticalStructure -------------");
        System.err.println(gs);

        System.err.println("------------- basic dependencies ---------------");
        System.err.println(StringUtils.join(gs.typedDependencies(false), "\n"));

        System.err.println("------------- non-collapsed dependencies (basic + extra) ---------------");
        System.err.println(StringUtils.join(gs.typedDependencies(true), "\n"));

        System.err.println("------------- collapsed dependencies -----------");
        System.err.println(StringUtils.join(gs.typedDependenciesCollapsed(true), "\n"));

        System.err.println("------------- collapsed dependencies tree -----------");
        System.err.println(StringUtils.join(gs.typedDependenciesCollapsedTree(), "\n"));

        System.err.println("------------- CCprocessed dependencies --------");
        System.err.println(StringUtils.join(gs.typedDependenciesCCprocessed(true), "\n"));

        System.err.println("-----------------------------------------------");
        }
    */    
       return;
    }
}
