#!/bin/bash

mvn clean package

java -jar target/SchwarzschildSimulatedAnnealing-1.0-SNAPSHOT-shaded.jar numberOfRuns 10000000 neighbourPeakScalingFactor 2.0 acceptanceProbabilityScalingFactor 1.0 temperatureScalingFactor 100.0 temperatureDivisor 20000