FROM ubuntu:focal

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
ENV PATH /opt/conda/bin:$PATH

# Install dependencies
RUN apt-get update --fix-missing && \
	apt-get install -y wget zip bzip2 curl git file vim && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

# Install miniconda3
RUN wget --quiet https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
	/bin/bash ~/miniconda.sh -b -p /opt/conda && \
    rm ~/miniconda.sh && \
    /opt/conda/bin/conda clean -tipsy && \
    ln -s /opt/conda/etc/profile.d/conda.sh /etc/profile.d/conda.sh && \
    echo ". /opt/conda/etc/profile.d/conda.sh" >> ~/.bashrc && \
	echo "conda activate && cd" >> ~/.bashrc

# Run and configurate conda environment
RUN /bin/bash -c "source activate && \
	conda config --add channels defaults && \
	conda config --add channels conda-forge && \
	conda config --add channels bioconda && \
	conda install -y openjdk=8 augustus mmseqs2 mafft iqtree"

# Download and unzip UFCG pipeline
RUN wget --quiet https://github.com/endixk/ufcg/releases/latest/download/UFCG.zip -O ~/ufcg.zip && \
	cd && \
	unzip ~/ufcg.zip && \
	rm ~/ufcg.zip

CMD [ "/bin/bash" ]
