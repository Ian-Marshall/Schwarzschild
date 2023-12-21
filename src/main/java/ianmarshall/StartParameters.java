package ianmarshall;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartParameters
{
	private static final Logger logger = LoggerFactory.getLogger(StartParameters.class);

	// The parameters' argument names and descriptions
	public static final String S_ARG_NAME_NUMBER_OF_RUNS = "numberOfRuns";
	private static final String S_ARG_DESC_NUMBER_OF_RUNS = "whole number";
	public static final String S_ARG_NAME_DECREMENT_FACTOR = "incrementFactor";
	private static final String S_ARG_DESC_DECREMENT_FACTOR = "decimal number";

	// The parameters' fields
	private int m_nRuns = 0;
	private double m_dblDecrementFactor = 0.0;

	public StartParameters()
	{
	}

	public int getNumberOfRuns()
	{
		return m_nRuns;
	}

	public double getDecrementFactor()
	{
		return m_dblDecrementFactor;
	}

	public void showUsage()
	{
		String sMsg = String.format(
		   "%nUsage"
		 + "%n-----"
		 + "%n  %s %s [%s] %s [%s]%n"
		 + "%n[%3$s] is the number of runs to be executed by the worker (calculation processor)."
		 + " This must be greater than zero."
		 + "%n[%5$s] is the multiplicative factor to be applied to the change of the values in a iteration."
		 + " This must be greater than zero and less than or equal to one."
		 + "%n",
		 SchwarzschildSimulatedAnnealing.class.getSimpleName(),
		 S_ARG_NAME_NUMBER_OF_RUNS, S_ARG_DESC_NUMBER_OF_RUNS,
		 S_ARG_NAME_DECREMENT_FACTOR, S_ARG_DESC_DECREMENT_FACTOR);

		logger.info(sMsg);
	}

	public String parseArguments(String[] asArgs)
	{
		StringBuilder sbError = new StringBuilder();
		logger.info(String.format("About to parse the command line arguments \"%s\".", Arrays.asList(asArgs)));

		if (asArgs.length == 4)
		{
			int nIndexArgNumberOfRuns = -1;
			int nIndexArgDecrementFactor = -1;

			for (int i = 0; i <= 2; i += 2)
			{
				if (S_ARG_NAME_NUMBER_OF_RUNS.equalsIgnoreCase(asArgs[i]))
					nIndexArgNumberOfRuns = i + 1;
				else if (S_ARG_NAME_DECREMENT_FACTOR.equalsIgnoreCase(asArgs[i]))
					nIndexArgDecrementFactor = i + 1;
			}

			if ((nIndexArgNumberOfRuns > -1) && (nIndexArgDecrementFactor > -1))
			{
				try
				{
					m_nRuns = Integer.parseInt(asArgs[nIndexArgNumberOfRuns]);
					m_dblDecrementFactor = Double.parseDouble(asArgs[nIndexArgDecrementFactor]);

					if (m_nRuns <= 0)
						sbError.append(String.format("The parameter \"%s\" of value %d must be greater than 0.",
						 S_ARG_NAME_NUMBER_OF_RUNS, m_nRuns));

					if ((m_dblDecrementFactor <= 0.0) || (m_dblDecrementFactor > 1.0))
					{
						if (sbError.length() > 0)
							sbError.append(" ");

						sbError.append(String.format("The parameter \"%s\" of value %f must be greater than 0.0"
						 + " and less than or equal to 1.0.", S_ARG_NAME_DECREMENT_FACTOR, m_dblDecrementFactor));
					}
				}
				catch (NumberFormatException e)
				{
					sbError.append("At least one of the parameters has an incorrect data type.");
				}
			}
			else
				sbError.append(String.format("At least one of the parameters \"%s\" and \"%s\" is missing.",
				 S_ARG_NAME_NUMBER_OF_RUNS, S_ARG_NAME_DECREMENT_FACTOR));
		}
		else
			sbError.append("Please specify exactly 2 parameters, each with one value.");

		if (sbError.length() > 0)
			sbError.append(" Please see the program's usage for details.");

		return sbError.toString();
	}
}
