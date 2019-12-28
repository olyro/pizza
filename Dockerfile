FROM anapsix/alpine-java:11.0.5-slim

RUN mkdir /pizza
RUN mkdir /pizza/db

ADD target/pizza-jar-with-dependencies.jar /pizza/pizza-jar-with-dependencies.jar
ADD public /pizza/public
ADD entrypoint.sh /pizza/entrypoint.sh

WORKDIR /pizza

EXPOSE 7000

ENTRYPOINT [ "./entrypoint.sh" ]
