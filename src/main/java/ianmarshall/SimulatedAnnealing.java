package ianmarshall;

import cern.colt.matrix.DoubleMatrix2D;

import ianmarshall.MetricComponents.MetricComponent;
import static ianmarshall.Worker.DerivativeLevel.None;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In order to apply the simulated annealing method to a specific problem, one must specify the following parameters:
 *   -  the state space:                     s
 *   -  the energy (goal) function:          E(s)
 *   -  the candidate generator procedure:   neighbour(s)
 *   -  the acceptance probability function: P(E, Enew, T)
 *   -  the annealing schedule:              temperature(iteration №).
 */
public class SimulatedAnnealing
{
	private static final Logger logger = LoggerFactory.getLogger(SimulatedAnnealing.class);
	private static final Random m_Random = new Random();    // Change this class for multi-threaded use

	/**
	 * The energy (goal) function.
	 * <br/>
	 * Calculate the energy of the state space, which is represented by the supplied tensor values and their derivatives.
	 * @param liG
	 *   The tensor values, with metric components for each value of radius.
	 * @param liGFirstDerivative
	 *   The 1st derivative of the tensor values.
	 * @param liGSecondDerivative
	 *   The 2nd derivative of the tensor values.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means no run has yet been executed.
	 * @return
	 *   The energy of the state space.
	 */
	public static double energy(List<MetricComponents> liG, List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative, int nRun)
	{
		double dblSumOfSquaresOfRicciTensorsOverAllR = 0.0;

		for (int i = 0; i < liG.size(); i++)
		{
			// 3 rows by 1 column
			DoubleMatrix2D dmRicci = Worker.calculateRicciTensorValues(liG, liGFirstDerivative, liGSecondDerivative, i);

			boolean bLog = (nRun <= 3) && ((i == 0) || (i == 3));
	 // boolean bLog = false;

			if (bLog)
			{
				String sMsg = String.format("%n  i = %d."
				 + "%n  dmRicci has elements:%n%s .%n",
				 i, dmRicci.toString());
				logger.info(sMsg);
			}

			for (int j = 0; j < dmRicci.rows(); j++)
			{
				double dblRicciTensor = dmRicci.get(j, 0);
				dblSumOfSquaresOfRicciTensorsOverAllR += dblRicciTensor * dblRicciTensor;
			}

		}

		return dblSumOfSquaresOfRicciTensorsOverAllR;
	}

	/**
	 * The candidate generator procedure.
	 * @param liG
	 *   The tensor values, with metric components for each value of radius.
	 * @return
	 *   The tensor values of the candidate, with metric components for each value of radius.
	 */
	public static List<MetricComponents> neighbour(List<MetricComponents> liG)
	{
		List<MetricComponents> liGResult = new ArrayList<>(liG);
		final double DBL_SCALING_FACTOR = 1.0;    // I might need to adjust this
		int nSize = liGResult.size();
		double dblStandardDeviationMax = nSize / 4.0;
		int nIndexCentre = m_Random.nextInt(nSize);

		double dblStandardDeviation = Math.floor(Math.random() * dblStandardDeviationMax);
		dblStandardDeviation = Math.max(0.1, Math.min(dblStandardDeviation, dblStandardDeviationMax));

		// Equally likely between -DBL_SCALING_FACTOR and +DBL_SCALING_FACTOR inclusive
		double dblDeltaPeak = DBL_SCALING_FACTOR * 2.0 * (Math.random() - 0.5);
		dblDeltaPeak = Math.max(-1.0, Math.min(dblDeltaPeak, 1.0));    // Strictly speaking, this line is unnecessary

		MetricComponent[] amcMetricComponents = MetricComponent.values();

		for (int i = 0; i < liGResult.size(); i++)
		{
			int nMCIndex = m_Random.nextInt(amcMetricComponents.length);
			MetricComponent mc = amcMetricComponents[nMCIndex];
			double dblExponent = (i - nIndexCentre) / dblStandardDeviation;
			dblExponent *= dblExponent;
			double dblDelta = dblDeltaPeak * Math.exp(dblExponent);

			double dblMC = Worker.getMetricComponentOfDerivativeLevel(liGResult, null, null, None, i, mc)
			 .getValue().doubleValue();
			Worker.setMetricComponentOfDerivativeLevel(liGResult, null, null, None, i, mc, dblMC + dblDelta);
		}

		return liGResult;
	}

	/**
	 * The acceptance probability function.
	 * <br/>
	 * Calculate the probability of the jump from the current to the new state.
	 * @param dblEnergyCurrent
	 *   The energy of the current state.
	 * @param dblEnergyNew
	 *   The energy of the new (proposed) state.
	 * @param dblTemperature
	 *   The simulated annealing temperature.
	 * @return
	 *   The probability of the jump from the current to the new state.
	 */
	public static double acceptanceProbability(double dblEnergyCurrent, double dblEnergyNew, double dblTemperature)
	{
		double result = 0.0;
		final double DBL_SCALING_FACTOR = 1.0;    // I might need to adjust this

		if (dblEnergyNew <= dblEnergyCurrent)
			result = 1.0;
		else if (dblTemperature <= 0.0)
			result = 0.0;
		else
			result = Math.exp(-DBL_SCALING_FACTOR *(dblEnergyNew - dblEnergyCurrent) / dblTemperature);  // exp(-(Enew - E)/T)

		return result;
	}

	/**
	 * The annealing schedule.
	 * <br/>
	 * Calculate the simulated annealing temperature.
	 * @param nIteration
	 *   The <code>1</code>-based iteration №.
	 * @param nRuns
	 *   The total № of runs (iterations).
	 * @return
	 * The simulated annealing temperature.
	 */
	public static double temperature(int nIteration, int nRuns)
	{
		double result = 1000.0 * (1.0 - (((double)(nIteration - 1)) / ((double)nRuns)));

		if (result < 0.0)
			result = 0.0;

		return result;
	}
}
