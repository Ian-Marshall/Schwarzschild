package ianmarshall;

import java.util.ArrayList;
import java.util.List;

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
	 * @return
	 *   The energy of the state space.
	 */
	public static double energy(List<MetricComponents> liG, List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative)
	{
		double result = 0.0;

		//
		// Add code here...
		//

		return result;
	}

	/**
	 * The candidate generator procedure.
	 * @param liG
	 *   The tensor values, with metric components for each value of radius.
	 * @param liGFirstDerivative
	 *   The 1st derivative of the tensor values.
	 * @param liGSecondDerivative
	 *   The 2nd derivative of the tensor values.
	 * @return
	 */
	public static List<MetricComponents> neighbour(List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative, List<MetricComponents> liGSecondDerivative)
	{
		List<MetricComponents> result = new ArrayList<>(liG);

		//
		// Add code here...
		//

		return result;
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
		final double DBL_SCALING_FACTOR = 1.0;    // I might need to adjust this
		double result = 0.0;

		if (dblEnergyNew <= dblEnergyCurrent)
			result = 1.0;
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
		return result;
	}
}
