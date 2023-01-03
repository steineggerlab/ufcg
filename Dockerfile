FROM ubuntu:focal

ENV LANG=C.UTF-8 LC_ALL=C.UTF-8
ENV PATH /opt/conda/bin:$PATH

# Install dependencies
RUN apt-get update --fix-missing && \
	apt-get install -y wget zip bzip2 curl git file vim iputils-ping && \
	apt-get clean && \
	rm -rf /var/lib/apt/lists/*

# Install miniconda3
RUN wget --quiet https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh -O ~/miniconda.sh && \
	/bin/bash ~/miniconda.sh -b -p /opt/conda && \
    rm ~/miniconda.sh && \
    /opt/conda/bin/conda clean --all -y && \
    ln -s /opt/conda/etc/profile.d/conda.sh /etc/profile.d/conda.sh && \
    echo ". /opt/conda/etc/profile.d/conda.sh" >> ~/.bashrc && \
	echo "conda activate && cd" >> ~/.bashrc

# Run and configurate conda environment
RUN /bin/bash -c "source activate && \
	conda config --add channels defaults && \
	conda config --add channels conda-forge && \
	conda config --add channels bioconda && \
	conda install -n base -y conda-libmamba-solver && \
	conda install -n base -y --solver=libmamba ufcg"

# Download minimum payloads
RUN wget --quiet https://ufcg.steineggerlab.workers.dev/payload/config.tar.gz && \
	wget --quiet https://ufcg.steineggerlab.workers.dev/payload/core.tar.gz && \
	wget --quiet https://ufcg.steineggerlab.workers.dev/payload/sample.tar.gz && \
	tar -xzf config.tar.gz -C /opt/conda/share/ufcg-1.0.3c-0/ && \
	tar -xzf core.tar.gz -C /opt/conda/share/ufcg-1.0.3c-0/ && \
	tar -xzf sample.tar.gz -C ~ && \
	rm *.tar.gz

CMD [ "/bin/bash" ]
