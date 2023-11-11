package ianmarshall;

import java.io.IOException;

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
			int nRuns = spStartParams.getNumberOfRuns();
			double dblIncrementFactor = spStartParams.getIncrementFactor();

			logger.info(String.format("Parameter values:"
			 + "%n  %s    = %d,"
			 + "%n  %s = %f."
			 + "%n%nTo pause execution enter \"P\"."
			 + "%nFrom a paused execution, enter \"S\" to stop execution and anything else to resume execution.",
			 StartParameters.S_ARG_NAME_NUMBER_OF_RUNS, nRuns, StartParameters.S_ARG_NAME_INCREMENT_FACTOR,
			 dblIncrementFactor));

			Supervisor supervisor = new Supervisor(spStartParams);
			WorkerResult wrResult = supervisor.execute();
			boolean bProcessingCompleted = wrResult.getProcessingCompleted();
			int nRun = wrResult.getRun();
			Throwable th = wrResult.getThrowable();
	 // List<MetricComponents> liG = wrResult.getMetricComponentsList();

			String sFormat;
			if (bProcessingCompleted)
				sFormat = "Processing completed. The latest run number executed was %d.";
			else
				sFormat = "Processing was stopped before it completed. The latest run number executed was %d.";

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
