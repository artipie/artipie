FROM continuumio/miniconda3:4.10.3

RUN conda install -y conda-build && conda install -y conda-verify && conda install -y anaconda-client
RUN anaconda config --set url "http://artipie.artipie:8080/my-conda/" -s && \
   echo "channels:\r\n  - http://artipie.artipie:8080/my-conda" > /root/.condarc
COPY ./run.sh /test/run.sh
COPY "./snappy-1.1.3-0.tar.bz2" "/test/snappy-1.1.3-0.tar.bz2"
WORKDIR /test
CMD "/test/run.sh"
