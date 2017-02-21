Relational Realizational Features for Sting-to-Tree SMT
=======================================================

Partially implements a Relational Realization feature for a string-to-tree MT system with target phrase-structure syntax. 
Extracts dependency relations from the phrase-structure trees of partial hypothesis while decoding. 
The aim is to score the target words and syntactic structures with models such as those proposed for Relational Realizational Parsing:

Tsarfaty, Reut and Sima'an, Khalil (2008): Relational-realizational Parsing. 
Proceedings of the 22Nd International Conference on Computational Linguistics - Volume 1 (COLING '08). Manchester, United Kingdom

http://dl.acm.org/citation.cfm?id=1599193

Moses feature:
--------------
https://github.com/moses-smt/mosesdecoder/blob/maria_HeadDrivenFeature/moses/FF/HeadFeature.cpp

https://github.com/moses-smt/mosesdecoder/blob/maria_HeadDrivenFeature/moses/FF/HeadFeature.h

https://github.com/moses-smt/mosesdecoder/blob/maria_HeadDrivenFeature/moses/FF/CreateJavaVM.cpp

https://github.com/moses-smt/mosesdecoder/blob/maria_HeadDrivenFeature/moses/FF/CreateJavaVM.h

Implements:

- Extracts the heads of constituents during decoding (while doing bottom up parsing) using Colins head rules.
- Interface to JVM to call a function processing phrase-structure trees using Stanford Dependency Parser (edu.stanford.nlp.trees.EnglishGrammaticalStructure)
- Selects target phrase-structure subtrees and returns the typed dependency tuples
- Scores tuples of (verb, dependency relation, argument) using a language model P(arg| verb,rel)

Relations.java: 

Function ProcessSentence() is called from the Moses feature. Gets as input a serialized phrase-structure tree and returns dependency relations extracted from this tree.
