# MA-JavaMicrobenchmarkAnalyses

A MicroAnalyzer plug-in to perform some analyses on Java microbenchmark configurations and compiler optimizations.

## How To Compile Sources

If you checked out the project from GitHub you can build the project with maven using:

```
mvn clean install
```

## Usage
Build the plugin jar and place it in the Java installation's */ext* folder. The return value of the overridden toString() method
corresponds to the parameter identifying the plug-in for MicroAnalyzer. The name of the dataset to perform analyses on is given
as a parameter to the AnalysisModule of MicroAnalyzer.
