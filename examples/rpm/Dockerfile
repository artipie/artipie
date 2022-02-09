FROM fedora:35

WORKDIR /test
RUN dnf install curl
RUN printf "[example]\nname=Example Repository\nbaseurl=http://artipie.artipie:8080/my-rpm\nenabled=1\ngpgcheck=0" > \
  /etc/yum.repos.d/example.repo
COPY ./run.sh /test/run.sh
COPY ./time-1.7-45.el7.x86_64.rpm /test/time-1.7-45.el7.x86_64.rpm
CMD "/test/run.sh"
