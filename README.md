# UFCG pipeline
[![Github release](https://img.shields.io/github/downloads/endix1029/ufcg/total?logo=github)](https://github.com/endixk/ufcg/releases/latest) [![Docker pulls](https://img.shields.io/docker/pulls/endix1029/ufcg?logo=docker)](https://hub.docker.com/repository/docker/endix1029/ufcg/)

## Introduction
UFCG pipeline provides methods for a genome-wide taxonomic profiling and annotation of your own biological sequences of Fungi.
 * [Homepage](https://ufcg.steineggerlab.com/)
 * [Manual](https://ufcg.steineggerlab.com/ufcg/manual)
 * [Tutorial](https://ufcg.steineggerlab.com/ufcg/tutorial)

## Requirements
* Java RE 8+ [Download](https://www.oracle.com/java/technologies/downloads/#java8)
* AUGUSTUS [Download](https://github.com/Gaius-Augustus/Augustus/releases/) [Reference](https://academic.oup.com/bioinformatics/article/24/5/637/202844)
* MMSeqs2 [Download](https://github.com/soedinglab/mmseqs2) [Reference](https://www.nature.com/articles/nbt.3988)
* MAFFT [Download](https://mafft.cbrc.jp/alignment/software/) [Reference](https://academic.oup.com/mbe/article/30/4/772/1073398)
* IQ-TREE [Download](http://www.iqtree.org/) [Reference](https://academic.oup.com/mbe/article/37/5/1530/5721363)

## Modules
### `profile`
UFCG `profile` extracts marker gene sequences from your own Fungal biological data, including genome sequences, transcriptome sequences, and proteome sequences.
* Interactive mode
~~~bash
java -jar UFCG.jar profile -u
~~~
* I/O mode
~~~bash
java -jar UFCG.jar profile -i <INPUT> -o <OUTPUT> [OPTIONS]
~~~

### `tree`
UFCG `tree` reconstructs the phylogenetic relationship of the set of marker gene profiles.
~~~bash
java -jar UFCG.jar tree -i <INPUT> -l <LEAF_FORMAT> [OPTIONS]
~~~

### `train`
UFCG `train` generates sequence model of your own fungal marker gene, even from a small set of seed sequences.
~~~bash
java -jar UFCG.jar train -i <INPUT> -g <REFERENCE> -o <OUTPUT> [OPTIONS]
~~~

### `align`
UFCG `align` conducts multiple sequence alignment of the genes from the set of marker gene profiles.
~~~bash
java -jar UFCG.jar align -i <INPUT> -o <OUTPUT> [OPTIONS]
~~~

## How to cite
Manuscript is under preparation.
