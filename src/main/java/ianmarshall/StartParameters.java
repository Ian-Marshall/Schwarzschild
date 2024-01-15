package ianmarshall;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartParameters
{
	private static final Logger logger = LoggerFactory.getLogger(StartParameters.class);
	private static int N_NUMBER_OF_ARGS = 4;


	// The parameters' argument names and data types

	public static final String S_ARG_NAME_NUMBER_OF_RUNS = "numberOfRuns";
	private static final String S_ARG_DATA_TYPE_NUMBER_OF_RUNS = "whole number";

	public static final String S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR = "neighbourPeakScalingFactor";
	private static final String S_ARG_DATA_TYPE_NEIGHBOUR_PEAK_SCALING_FACTOR = "decimal number";

	public static final String S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR = "acceptanceProbabilityScalingFactor";
	private static final String S_ARG_DATA_TYPE_ACCEPTANCE_PROBILITY_SCALING_FACTOR = "decimal number";

	public static final String S_ARG_NAME_TEMPERATURE_SCALING_FACTOR = "temperatureScalingFactor";
	private static final String S_ARG_DATA_TYPE_TEMPERATURE_SCALING_FACTOR = "decimal number";


	// The parameters' fields
	private int m_nRuns = 0;
	private double m_dblNeighbourPeakScalingFactor = 0.0;
	private double m_dblAcceptanceProbabilityScalingFactor = 0.0;
	private double m_dblTemperatureScalingFactor = 0.0;

	public StartParameters()
	{
	}

	public int getNumberOfRuns()
	{
		return m_nRuns;
	}

	public double getNeighbourPeakScalingFactor()
	{
		return m_dblNeighbourPeakScalingFactor;
	}

	public double getAcceptanceProbabilityScalingFactor()
	{
		return m_dblAcceptanceProbabilityScalingFactor;
	}

	public double getTemperatureScalingFactor()
	{
		return m_dblTemperatureScalingFactor;
	}

	public void showUsage()
	{
		String sMsg = String.format(
		   "%nUsage"
		 + "%n-----"
		 + "%n  %s %s [%s] %s [%s] %s [%s] %s [%s]%n"
		 + "%n[%2$s] is the number of runs to be executed by the worker (calculation processor)."
		 + " This must be greater than zero."
		 + "%n[%4$s] is the scaling factor to be applied to changes of neighbouring values in a iteration."
		 + " This must be greater than zero."
		 + "%n[%6$s] is the scaling factor to be used when calculating the probability of accepting a neighbouring state."
		 + " This must be greater than zero."
		 + "%n[%8$s] is the scaling factor to be used when calculating the annealing temperature."
		 + " This must be greater than zero."
		 + "%n",
		 SchwarzschildSimulatedAnnealing.class.getSimpleName(),
		 S_ARG_NAME_NUMBER_OF_RUNS,                      S_ARG_DATA_TYPE_NUMBER_OF_RUNS,
		 S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR,       S_ARG_DATA_TYPE_NEIGHBOUR_PEAK_SCALING_FACTOR,
		 S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR, S_ARG_DATA_TYPE_ACCEPTANCE_PROBILITY_SCALING_FACTOR,
		 S_ARG_NAME_TEMPERATURE_SCALING_FACTOR,          S_ARG_DATA_TYPE_TEMPERATURE_SCALING_FACTOR);

		logger.info(sMsg);
	}

	public String parseArguments(String[] asArgs)
	{
		StringBuilder sbError = new StringBuilder();
		logger.info(String.format("About to parse the command line arguments \"%s\".", Arrays.asList(asArgs)));

		if (asArgs.length == 2 * N_NUMBER_OF_ARGS)
		{
			int nIndexArgNumberOfRuns = -1;
			int nIndexArgNeighbourPeakScalingFactor = -1;
			int nIndexArgAcceptanceProbabilityScalingFactor = -1;
			int nIndexArgTemperatureScalingFactor = -1;

			for (int i = 0; i < N_NUMBER_OF_ARGS; i++)
			{
				int nIndexArgName = 2 * i;

				if (S_ARG_NAME_NUMBER_OF_RUNS.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgNumberOfRuns = nIndexArgName + 1;
				else if (S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgNeighbourPeakScalingFactor = nIndexArgName + 1;
				else if (S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgAcceptanceProbabilityScalingFactor = nIndexArgName + 1;
				else if (S_ARG_NAME_TEMPERATURE_SCALING_FACTOR.equalsIgnoreCase(asArgs[nIndexArgName]))
					nIndexArgTemperatureScalingFactor = nIndexArgName + 1;
			}

			if ((nIndexArgNumberOfRuns > -1) && (nIndexArgNeighbourPeakScalingFactor > -1)
			 && (nIndexArgAcceptanceProbabilityScalingFactor > -1) && (nIndexArgTemperatureScalingFactor > -1))
			{
				try
				{
					m_nRuns = Integer.parseInt(asArgs[nIndexArgNumberOfRuns]);
					m_dblNeighbourPeakScalingFactor = Double.parseDouble(asArgs[nIndexArgNeighbourPeakScalingFactor]);
					m_dblAcceptanceProbabilityScalingFactor =
					 Double.parseDouble(asArgs[nIndexArgAcceptanceProbabilityScalingFactor]);
					m_dblTemperatureScalingFactor = Double.parseDouble(asArgs[nIndexArgTemperatureScalingFactor]);

					if (m_nRuns <= 0)
						sbError.append(String.format("The parameter \"%s\" of value %d must be greater than 0.",
						 S_ARG_NAME_NUMBER_OF_RUNS, m_nRuns));

					if (m_dblNeighbourPeakScalingFactor <= 0.0)
					{
						if (sbError.length() > 0)
							sbError.append(" ");

						sbError.append(String.format("The parameter \"%s\" of value %f must be greater than 0.0 .",
						 S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR, m_dblNeighbourPeakScalingFactor));
					}

					if (m_dblAcceptanceProbabilityScalingFactor <= 0.0)
					{
						if (sbError.length() > 0)
							sbError.append(" ");

						sbError.append(String.format("The parameter \"%s\" of value %f must be greater than 0.0 .",
						 S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR, m_dblAcceptanceProbabilityScalingFactor));
					}

					if (m_dblTemperatureScalingFactor <= 0.0)
					{
						if (sbError.length() > 0)
							sbError.append(" ");

						sbError.append(String.format("The parameter \"%s\" of value %f must be greater than 0.0 .",
						 S_ARG_NAME_TEMPERATURE_SCALING_FACTOR, m_dblTemperatureScalingFactor));
					}
				}
				catch (NumberFormatException e)
				{
					sbError.append("At least one of the parameters has an incorrect data type.");
				}
			}
			else
				sbError.append(String.format("At least one of the parameters \"%s\", \"%s\", \"%s\" and \"%s\" is missing.",
				 S_ARG_NAME_NUMBER_OF_RUNS, S_ARG_NAME_NEIGHBOUR_PEAK_SCALING_FACTOR,
				 S_ARG_NAME_ACCEPTANCE_PROBILITY_SCALING_FACTOR, S_ARG_NAME_TEMPERATURE_SCALING_FACTOR));
		}
		else
			sbError.append(String.format("Please specify exactly %d parameters, each with one value.", N_NUMBER_OF_ARGS));

		if (sbError.length() > 0)
			sbError.append(" Please see the program's usage for details.");

		return sbError.toString();
	}
}
