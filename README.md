# UFCG pipeline
[![Build](https://img.shields.io/github/actions/workflow/status/steineggerlab/ufcg/maven-build.yml)](https://github.com/steineggerlab/ufcg/actions)
[![License](https://img.shields.io/github/license/steineggerlab/ufcg)](https://github.com/steineggerlab/ufcg/blob/main/LICENSE.md)
[![Bioconda](https://img.shields.io/conda/dn/bioconda/ufcg?logo=anaconda)](https://anaconda.org/bioconda/ufcg)
[![Docker](https://img.shields.io/docker/pulls/endix1029/ufcg?logo=docker)](https://hub.docker.com/repository/docker/endix1029/ufcg/)

UFCG pipeline provides methods for a genome-wide taxonomic profiling and annotation of your own biological sequences of Fungi.
 * [Homepage](https://ufcg.steineggerlab.com/)
 * [Preprint](https://www.biorxiv.org/content/10.1101/2022.08.16.504087v1)
 * [Publication](https://academic.oup.com/nar/advance-article/doi/10.1093/nar/gkac894/6769744)

### How to cite
Kim, D., Gilchrist, C.L.M., Chun, J. & Steinegger, M. (2023) UFCG: database of universal fungal core genes and pipeline for genome-wide phylogenetic analysis of fungi. _Nucleic Acids Research_, _51_(D1), D777-D784, doi:10.1093/nar/gkac894.

<p align="center"><img src="https://github.com/steineggerlab/ufcg/assets/49298377/7aa5a1d7-96b0-4f1e-b151-60220f796d94" height="256" /></p>

## Quick start with conda
~~~bash
conda install -y ufcg # conda-libmamba-solver recommended
ufcg download -t minimum
ufcg -h
~~~

## Quick start with docker 
~~~bash
docker pull endix1029/ufcg:latest
docker run -it endix1029/ufcg:latest
ufcg -h
~~~

## Modules
### `profile`
UFCG `profile` extracts marker gene sequences from your own Fungal biological data, including genome sequences, transcriptome sequences, and proteome sequences.
* Interactive mode
~~~bash
ufcg profile -u
~~~
* I/O mode
~~~bash
ufcg profile -i <INPUT> -o <OUTPUT> [OPTIONS]
~~~

### `tree`
UFCG `tree` reconstructs the phylogenetic relationship of the set of marker gene profiles.
~~~bash
ufcg tree -i <INPUT> -o <OUTPUT> [OPTIONS]
~~~

### `train`
UFCG `train` generates sequence model of your own fungal marker gene, even from a small set of seed sequences.
~~~bash
ufcg train -i <INPUT> -g <REFERENCE> -o <OUTPUT> [OPTIONS]
~~~

### `align`
UFCG `align` conducts multiple sequence alignment of the genes from the set of marker gene profiles.
~~~bash
ufcg align -i <INPUT> -o <OUTPUT> [OPTIONS]
~~~

## Build from source
### Requirements
* Java 8 or higher
* Maven 3.9.4 or higher

### Build
~~~bash
git clone https://github.com/steineggerlab/ufcg.git
cd ufcg
mvn clean package appassembler:assemble
./target/bin/ufcg -h
~~~
