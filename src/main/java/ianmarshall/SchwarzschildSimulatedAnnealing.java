package ianmarshall;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchwarzschildSimulatedAnnealing
{
	private static final Logger logger = LoggerFactory.getLogger(SchwarzschildSimulatedAnnealing.class);

	public SchwarzschildSimulatedAnnealing()
	{
	}

	public static void main(String[] asArgs) throws IOException
	{
		SchwarzschildSimulatedAnnealing ssa = new SchwarzschildSimulatedAnnealing();
		ssa.execute(asArgs);
	}

	private void execute(String[] asArgs) throws IOException
	{
		StartParameters spStartParams = new StartParameters();
		spStartParams.showUsage();

		String sError = spStartParams.parseArguments(asArgs);
		if (sError.isEmpty())
		{
			int    nRuns                                 = spStartParams.getNumberOfRuns();
			double dblNeighbourPeakScalingFactor         = spStartParams.getNeighbourPeakScalingFactor();
			double dblAcceptanceProbabilityScalingFactor = spStartParams.getAcceptanceProbabilityScalingFactor();
			double dblTemperatureScalingFactor           = spStartParams.getTemperatureScalingFactor();

			int nMaxWidth = Collections.max(Arrays.asList(
			 StartParameters.S_ARG_NAME_NUMBER_OF_RUNS.length(),
			 StartParameters.S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR.length(),
			 StartParameters.S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR.length(),
			 StartParameters.S_ARG_NAME_TEMPERATURE_SCALING_FACTOR.length()));

			String sFormat = String.format("Parameter values:"
			 + "%%n  %%%1$ss = %%d,"
			 + "%%n  %%%1$ss = %%f,"
			 + "%%n  %%%1$ss = %%f,"
			 + "%%n  %%%1$ss = %%f."
			 + "%%n%%nTo pause execution enter \"P\"."
			 + "%%nFrom a paused execution, enter \"S\" to stop execution and anything else to resume execution.", nMaxWidth);

			logger.info(String.format(sFormat,
			 StartParameters.S_ARG_NAME_NUMBER_OF_RUNS,                      nRuns,
			 StartParameters.S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR,       dblNeighbourPeakScalingFactor,
			 StartParameters.S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR, dblAcceptanceProbabilityScalingFactor,
			 StartParameters.S_ARG_NAME_TEMPERATURE_SCALING_FACTOR,          dblTemperatureScalingFactor));

			Supervisor supervisor = new Supervisor(spStartParams);
			WorkerResult wrResult = supervisor.execute();
			boolean bProcessingCompleted = wrResult.getProcessingCompleted();
			int nRun = wrResult.getRun();
			Throwable th = wrResult.getThrowable();
	 // List<MetricComponents> liG = wrResult.getMetricComponentsList();

			if (bProcessingCompleted)
				sFormat = "Processing has completed.";
			else
				sFormat = "Processing was stopped before it completed.";

			sFormat += " The latest run number executed was %d.";
			logger.info(String.format(sFormat, nRun));

			StringBuilder sb = new StringBuilder();

			while (th != null)
			{
				if (sb.length() == 0)
					sb.append(String.format("An exception or error was thrown: "));
				else
					sb.append(String.format("caused by: "));

				sb.append(String.format("\"%s\"%n with stack trace:%n", th.toString()));
				StackTraceElement[] asteStackTraceElements = th.getStackTrace();

				for (StackTraceElement steStackTraceElement: asteStackTraceElements)
					sb.append(String.format("  %s%n", steStackTraceElement.toString()));

				th = th.getCause();
			}

			sError = sb.toString();
		}

		if (!sError.isEmpty())
			logger.error(sError);
	}
}
