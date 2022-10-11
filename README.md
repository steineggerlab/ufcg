# UFCG pipeline
<!-- [![Github release](https://img.shields.io/github/downloads/endix1029/ufcg/total?logo=github)](https://github.com/endixk/ufcg/releases/latest) [![Docker pulls](https://img.shields.io/docker/pulls/endix1029/ufcg?logo=docker)](https://hub.docker.com/repository/docker/endix1029/ufcg/) -->

UFCG pipeline provides methods for a genome-wide taxonomic profiling and annotation of your own biological sequences of Fungi.
 * [Homepage](https://ufcg.steineggerlab.com/)
 * [Preprint](https://www.biorxiv.org/content/10.1101/2022.08.16.504087v1)

## Quick start with conda
~~~bash
conda create -n ufcg -c bioconda -c conda-forge openjdk=8 augustus=3.4.0 mmseqs2 mafft iqtree
conda activate ufcg
wget -O UFCG.zip https://github.com/endixk/ufcg/releases/latest/download/UFCG.zip
unzip UFCG.zip && cd UFCG
java -jar UFCG.jar
~~~

## Quick start with docker 
~~~bash
docker pull endix1029/ufcg:latest
docker run -it endix1029/ufcg:latest
cd UFCG
java -jar UFCG.jar
~~~

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
[Kim, D., Gilchrist, C.L.M., Chun, J. & Steinegger, M. UFCG: database of universal fungal core genes and pipeline for genome-wide phylogenetic analysis of fungi. bioRxiv (2022).](https://www.biorxiv.org/content/10.1101/2022.08.16.504087v1)
