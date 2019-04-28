FROM adoptopenjdk/openjdk11:latest
RUN mkdir /opt/app
COPY build/libs/financeviews-0.0.1-SNAPSHOT.jar /opt/app
CMD ["java", "-jar", "/opt/app/financeviews-0.0.1-SNAPSHOT.jar"]