FROM fedora:35

WORKDIR /test
COPY ./example.repo /etc/yum.repos.d/example.repo
COPY ./run.sh /test/run.sh
COPY "./time-1.7-45.el7.x86_64.rpm" "/test/time-1.7-45.el7.x86_64.rpm"
CMD "/test/run.sh"
