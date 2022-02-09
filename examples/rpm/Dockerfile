FROM fedora:35
RUN printf "[example]\nname=Example Repository\nbaseurl=http://artipie:8080/my-rpm\nenabled=1\ngpgcheck=0" > \
  /etc/yum.repos.d/example.repo
COPY ./run.sh /test/run.sh
CMD "/test/run.sh"
