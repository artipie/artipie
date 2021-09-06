FROM continuumio/miniconda3:4.10.3

RUN conda install -y conda-build
RUN conda install -y conda-verify
RUN conda install -y anaconda-client
RUN anaconda config --set url "http://artipie:8080/my-conda/" -s
RUN conda config --set anaconda_upload yes
RUN echo "channels:\r\n  - http://artipie:8080/my-conda" > /root/.condarc
COPY ./run.sh /test/run.sh
COPY ./sample-project /test/sample-project
WORKDIR /test
CMD "/test/run.sh"
