# UFCG pipeline
[![Github](https://img.shields.io/github/downloads/endix1029/ufcg/total?logo=github)](https://github.com/steineggerlab/ufcg/releases/latest) [![Bioconda](https://img.shields.io/conda/dn/bioconda/ufcg?logo=anaconda)](https://anaconda.org/bioconda/ufcg) [![Docker](https://img.shields.io/docker/pulls/endix1029/ufcg?logo=docker)](https://hub.docker.com/repository/docker/endix1029/ufcg/)

UFCG pipeline provides methods for a genome-wide taxonomic profiling and annotation of your own biological sequences of Fungi.
 * [Homepage](https://ufcg.steineggerlab.com/)
 * [Preprint](https://www.biorxiv.org/content/10.1101/2022.08.16.504087v1)
 * [Publication](https://academic.oup.com/nar/advance-article/doi/10.1093/nar/gkac894/6769744)

### How to cite
Kim, D., Gilchrist, C.L.M., Chun, J. & Steinegger, M. (2023) UFCG: database of universal fungal core genes and pipeline for genome-wide phylogenetic analysis of fungi. _Nucleic Acids Research_, _51_(D1), D777-D784, doi:10.1093/nar/gkac894.

## Quick start with conda
~~~bash
mamba install -y ufcg # conda install also works (slow)
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
ufcg tree -i <INPUT> -l <LEAF_FORMAT> [OPTIONS]
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

