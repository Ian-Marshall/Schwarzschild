package ianmarshall;

import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.DoubleMatrix2D;

import ianmarshall.MetricComponents.MetricComponent;
import static ianmarshall.MetricComponents.MetricComponent.A;
import static ianmarshall.MetricComponents.MetricComponent.B;
import static ianmarshall.Worker.DerivativeLevel.First;
import static ianmarshall.Worker.DerivativeLevel.None;
import static ianmarshall.Worker.DerivativeLevel.Second;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Worker implements Runnable
{
	public enum DerivativeLevel
	{
		None, First, Second;

		public DerivativeLevel getDiffential()
		{
			DerivativeLevel dlResult = None;

			switch (this)
			{
				case None:
					dlResult = First;
					break;
				case First:
					dlResult = Second;
					break;
				case Second:
				default:
					throw new RuntimeException("Invalid derivative level.");
			}

			return dlResult;
		};

		public DerivativeLevel getIntegral()
		{
			DerivativeLevel dlResult = None;

			switch (this)
			{
				case First:
					dlResult = None;
					break;
				case Second:
					dlResult = First;
					break;
				case None:
				default:
					throw new RuntimeException("Invalid derivative level.");
			}

			return dlResult;
		};
	}

	public class WorkerUncaughtExceptionHandler implements UncaughtExceptionHandler
	{
		public WorkerUncaughtExceptionHandler()
		{
		}

		@Override
		public void uncaughtException(Thread t, Throwable th)
		{
			m_WorkerResult = new WorkerResult(m_bProcessingCompleted, th, m_nRun, m_liG);
			m_bStopped = true;
		}

	}

	private static final Logger logger = LoggerFactory.getLogger(Worker.class);
	private static final StringBuilder s_sbMoveLog = new StringBuilder();    // Refactor this for multi-instance use
	private int m_nRun = 0;
	private int m_nRuns = 0;

	// These are tensor values, with metric components for each value of radius
	private List<MetricComponents> m_liG = null;
	private List<MetricComponents> m_liGFirstDerivative = null;
	private List<MetricComponents> m_liGSecondDerivative = null;

	private volatile boolean m_bStopping = false;
	private boolean m_bStopped = false;
	private boolean m_bProcessingCompleted = false;
	private WorkerUncaughtExceptionHandler m_wuehExceptionHandler = null;
	private WorkerResult m_WorkerResult = null;

	private SimulatedAnnealing m_saSimulatedAnnealing = null;
	private double m_dblEnergyCurrent = -1.0;

	/**
	 * The constructor.
	 * @param spStartParameters
	 *   The the application's start parameters.
	 * @param nRun
	 *   The number of runs already executed. A value of <code>0</code> means no run has yet been executed.
	 * @param liG
	 *   If not <code>null</code> then use this to set the metric tensor values, otherwise calculate the initial values.
	 */
	public Worker(StartParameters spStartParameters, int nRun, List<MetricComponents> liG)
	{
		m_nRun = nRun;
		m_nRuns = spStartParameters.getNumberOfRuns();
		m_liG = liG;
		m_bStopping = false;
		m_bStopped = false;
		m_bProcessingCompleted = false;
		m_wuehExceptionHandler = new WorkerUncaughtExceptionHandler();
		m_saSimulatedAnnealing = new SimulatedAnnealing(spStartParameters);
	}

	public void stopExecution()
	{
		m_bStopping = true;
		logger.info(String.format("Stopping run number %d...", m_nRun));
	}

	/**
	 * @return
	 *   Whether working has stopped, whether all processing has been completed or not.
	 */
	public boolean getStopped()
	{
		return m_bStopped;
	}

	public WorkerUncaughtExceptionHandler getWorkerUncaughtExceptionHandler()
	{
		return m_wuehExceptionHandler;
	}

	public WorkerResult getWorkerResult()
	{
		return m_WorkerResult;
	}

	@Override
	public void run()
	{
		while ((!m_bStopping) && (m_nRun < m_nRuns))
		{
			m_nRun++;
	 // logger.info(String.format("Started run number %d.", m_nRun));

			if (m_liG == null)
			{
				initialiseMetricTensors();
				calculateAllDifferentialsForAllValues(m_liG, m_liGFirstDerivative, m_liGSecondDerivative);
			}

			if (m_nRun == 1)    // then the current energy has not been calculated yet
				m_dblEnergyCurrent = m_saSimulatedAnnealing.energy(m_liG, m_liGFirstDerivative, m_liGSecondDerivative, m_nRun);

			List<MetricComponents> liGNew = m_saSimulatedAnnealing.neighbour(m_liG);
			List<MetricComponents> liGNewFirstDerivative = MetricComponents.deepCopyMetricComponents(m_liGFirstDerivative);
			List<MetricComponents> liGNewSecondDerivative = MetricComponents.deepCopyMetricComponents(m_liGSecondDerivative);
			calculateAllDifferentialsForAllValues(liGNew, liGNewFirstDerivative, liGNewSecondDerivative);
			double dblEnergyNew = m_saSimulatedAnnealing.energy(liGNew, liGNewFirstDerivative, liGNewSecondDerivative,
			 m_nRun);
			double dblTemperature = m_saSimulatedAnnealing.temperature(m_nRun, m_nRuns);
			double dblProbability = m_saSimulatedAnnealing.acceptanceProbability(m_dblEnergyCurrent, dblEnergyNew,
			 dblTemperature);
			boolean bAcceptMove = Math.random() < dblProbability;
			String sLogEntry = null;

			if (bAcceptMove)
			{
				sLogEntry = String.format("Run number %d:"
				 + "    ***  Accepted move from energy %f to %f at temperature %f with probability %.5f.  ***",
				 m_nRun, m_dblEnergyCurrent, dblEnergyNew, dblTemperature, dblProbability);

				int nStartLength = s_sbMoveLog.length();
				s_sbMoveLog.append(String.format("%n  run %d: %s", m_nRun, sLogEntry));

				if (nStartLength == 0)
				{
					String sRemove = String.format("%n");
					int nLengthRemove = sRemove.length();
					s_sbMoveLog.delete(0, nLengthRemove);
				}

				m_liG = liGNew;
				m_liGFirstDerivative = liGNewFirstDerivative;
				m_liGSecondDerivative = liGNewSecondDerivative;
				m_dblEnergyCurrent = dblEnergyNew;

				String sLogMessage = m_saSimulatedAnnealing.popLatestLogMessage();
				logger.info(sLogMessage);
			}
			else if (dblProbability >= 0.01)
				sLogEntry = String.format("Run number %d:"
				 + " rejected move from energy %f to %f with probability %.5f.",
				 m_nRun, m_dblEnergyCurrent, dblEnergyNew, dblProbability);

			if (sLogEntry != null)
			{
				logger.info(sLogEntry);
		 // logger.info(String.format("Completed run number %d with current energy %f.", m_nRun, m_dblEnergyCurrent));
			}

	 // logger.info(String.format("Completed run number %d with current energy %f.", m_nRun, m_dblEnergyCurrent));
		}

		if (m_nRun >= m_nRuns)
		{
			logger.info(String.format("Move log is:%n%s", s_sbMoveLog));
			s_sbMoveLog.setLength(0);

			m_bProcessingCompleted = true;
			logger.info("All processing has been completed.");
		}
		else
			logger.info("Stopped before all processing completed.");

		reportFinalTensorValues();
		m_WorkerResult = new WorkerResult(m_bProcessingCompleted, null, m_nRun, m_liG);
		m_bStopped = true;
	}

	/**
	 * Initialise the metric tensor, and its first and second derivatives with respect to radius,
	 * with start values for logarithmically-graduated radius values.
	 */
	private void initialiseMetricTensors()
	{
		m_liG = new ArrayList<>();
		m_liGFirstDerivative = new ArrayList<>();
		m_liGSecondDerivative = new ArrayList<>();
		logger.info(String.format("Initialising the metric components (in the format \"index, r, A, B\"):"));

		final double DBL_R_MIN = 1.01;
		final double DBL_R_MAX = 100.0;
		final double DBL_STEP_FACTOR_RADIUS = 1.014;
		double dblR = DBL_R_MIN;
		int i = 0;
		boolean bLoop = true;
		boolean bOneMoreLoop = false;

		while (bLoop)
		{
			if (bOneMoreLoop)
				bLoop = false;

			// The start-state energy is ~701.348
	 // double dblA =   1.0 * dblR / DBL_R_MAX;
	 // double dblB = - 1.0 * DBL_R_MAX / dblR;

	 // double dblA = dblR / DBL_R_MAX;
	 // double dblB = -DBL_R_MAX / dblR;

			// The start-state energy is 165.750
	 // double dblA = 0.5;
	 // double dblB = -2.0;

			// The start-state energy is 0.00
	 // double dblA = 1.0;
	 // double dblB = -1.0;

			double dblA =   1.0 * dblR / DBL_R_MAX;
			double dblB =  -1.0 * DBL_R_MAX / dblR;

			m_liG.add(new MetricComponents(dblR, dblA, dblB));
			m_liGFirstDerivative.add(new MetricComponents(dblR, 0.0, 0.0));
			m_liGSecondDerivative.add(new MetricComponents(dblR, 0.0, 0.0));

			if ((i >= 662) || ((i % 100) == 0))
				logger.info(String.format("  %3d, %,12f, %,12f, %,12f", i, dblR, dblA, dblB));

			double dblRNew = ((dblR  - 1.0) * DBL_STEP_FACTOR_RADIUS) + 1.0;

			if (dblRNew < DBL_R_MAX)
				dblR = dblRNew;
			else if (dblR < DBL_R_MAX)
			{
				// We shall loop once more only, and then not rely on comparison precision
				dblR = DBL_R_MAX;
				bOneMoreLoop = true;
			}
			else
				bLoop = false;

			i++;
		}

		logger.info("The metric components have been initialised.");
	}

	/**
	 * Calculate the first and second differentials of all the metric tensor components with respect to radius.
	 * <br>
	 * All of the parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius values. This number of elements must be at least 5.
	 * @param liG
	 *   The metric tensor components.
	 * @param liGFirstDerivative
	 *   The first differential of the metric tensor components with respect to radius.
	 * @param liGSecondDerivative
	 *   The second differential of the metric tensor components with respect to radius.
	 */
	private void calculateAllDifferentialsForAllValues(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative)
	{
		final int N = liG.size() - 1;
		DerivativeLevel[] adlDerivativeLevel = {First, Second};

		for (int i = 0; i <= N; i++)
		{
			MetricComponents mc1 = liGFirstDerivative.get(i);
			mc1.setA(0.0);
			mc1.setB(0.0);

			MetricComponents mc2 = liGSecondDerivative.get(i);
			mc2.setA(0.0);
			mc2.setB(0.0);
		}

		for (MetricComponent mcMetricComponent: MetricComponent.values())
			for (DerivativeLevel dlDerivativeLevel: adlDerivativeLevel)
				switch (dlDerivativeLevel)
				{
					case First:
							for (int i = 0; i <= N; i++)
								calculateDifferentialOfMetricComponent(liG, liGFirstDerivative,
								 liGSecondDerivative, dlDerivativeLevel, i, mcMetricComponent);
						break;
					case Second:
							for (int i = 1; i <= N - 1; i++)
								calculateDifferentialOfMetricComponent(liG, liGFirstDerivative,
								 liGSecondDerivative, dlDerivativeLevel, i, mcMetricComponent);

							for (int i = 0; i <= N; i+= N)
								calculateDifferentialOfMetricComponent(liG, liGFirstDerivative,
								 liGSecondDerivative, dlDerivativeLevel, i, mcMetricComponent);
							break;
					default:
						throw new RuntimeException("Wrong derivative level.");
				}
	}

	/**
	 * Calculate the specified level of differential of the specified metric component and store it
	 * in the appropriate list supplied.
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius values. This number of elements must be at least 5.
	 * <br>
	 * All the results of this method for lower order (weaker) derivatives must already have been stored
	 * before this method is called.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be calculated.
	 * @param nIndex
	 *   The zero-based index value of the metric component, the differential of which is to be calculated.
	 * @param mcMetricComponent
	 *   The metric component, the differential of which is to be calculated.
	 */
	private void calculateDifferentialOfMetricComponent(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative,
	 DerivativeLevel dlDerivativeLevel, int nIndex,
	 MetricComponent mcMetricComponent)
	{
			double dblValue = differentialOfMetricComponent(liG,
			 liGFirstDerivative, liGSecondDerivative, dlDerivativeLevel, nIndex,
			 mcMetricComponent);

			setMetricComponentOfDerivativeLevel(liG, liGFirstDerivative,
			 liGSecondDerivative, dlDerivativeLevel, nIndex, mcMetricComponent,
			 dblValue);
	}

	/**
	 * Calculate the specified level of differential of the specified metric component.
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius values. This number of elements must be at least 5.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be calculated.
	 * @param nIndex
	 *   The zero-based index value of the metric component, the differential of which is to be calculated.
	 * @param mcMetricComponent
	 *   The metric component, the differential of which is to be calculated.
	 * @return
	 *   The specified level of differential of the specified metric component with respect to radius,
	 *   calculated at the radius of the entry of the list of the given index.
	 */
	private double differentialOfMetricComponent(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative,
	 DerivativeLevel dlDerivativeLevel, int nIndex,
	 MetricComponent mcMetricComponent)
	{
		double dblResult = 0.0;

		// The middle elements (index 2) are those of the index supplied.
		// The other (side) elements are those of either side of the index supplied,
		// if they exist, otherwise they will be zero.
		double[] adblR                         = new double[5];
		double[] adblComponent                 = new double[5];
		double[] adblComponentSecondDerivative = new double[5];

		final int N = liG.size() - 1;    // The maximum index value
		int n = 0;                       // The array index

		// Load the arrays
		for (int i = nIndex - 2; i <= nIndex + 2; i++)
		{
			if ((i >= 0) && (i <= N))
			{
				Entry<Double, Double> entry = getMetricComponentOfDerivativeLevel(liG,
				 liGFirstDerivative, liGSecondDerivative, DerivativeLevel.None, i,
				 mcMetricComponent);

				adblR[n] = entry.getKey().doubleValue();
				adblComponent[n] = entry.getValue().doubleValue();

				double dbl = 0.0;
				if ((dlDerivativeLevel == Second) && ((nIndex == 0) || (nIndex == N)))
				{
					Entry<Double, Double> entryGSecondDerivative =
					 getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative,
						liGSecondDerivative, dlDerivativeLevel, i, mcMetricComponent);

					Double dblValue = entryGSecondDerivative.getValue();
					if (dblValue != null)
						dbl = dblValue.doubleValue();
				}

				adblComponentSecondDerivative[n] = dbl;
			}
			else
			{
				adblR[n]                         = 0.0;
				adblComponent[n]                 = 0.0;
				adblComponentSecondDerivative[n] = 0.0;
			}

			n++;
		}

		if ((dlDerivativeLevel == First) && (nIndex > 0) && (nIndex < N))
		{
			dblResult = 0.5 * (((adblComponent[3] - adblComponent[2]) / (adblR[3] - adblR[2]))
			                 + ((adblComponent[2] - adblComponent[1]) / (adblR[2] - adblR[1])));
		}
		else if ((dlDerivativeLevel == Second) && (nIndex > 0) && (nIndex < N))
		{
			dblResult = 2.0 * (((adblComponent[3] - adblComponent[2]) / (adblR[3] - adblR[2]))
			                 - ((adblComponent[2] - adblComponent[1]) / (adblR[2] - adblR[1])))
											/ (adblR[3] - adblR[1]);
		}
		else if ((dlDerivativeLevel == First) && (nIndex == 0))
		{
			dblResult = (2 * (adblComponent[3] - adblComponent[2]) / (adblR[3] - adblR[2]))
								- (    (adblComponent[4] - adblComponent[2]) / (adblR[4] - adblR[2]));
		}
		else if ((dlDerivativeLevel == First) && (nIndex == N))
		{
			dblResult = (2 * (adblComponent[2] - adblComponent[1]) / (adblR[2] - adblR[1]))
								- (    (adblComponent[2] - adblComponent[0]) / (adblR[2] - adblR[0]));
		}
		else if ((dlDerivativeLevel == Second) && (nIndex == 0))
		{
			dblResult = (2 * adblComponentSecondDerivative[3]) - adblComponentSecondDerivative[4];
		}
		else if ((dlDerivativeLevel == Second) && (nIndex == N))
		{
			dblResult = (2 * adblComponentSecondDerivative[1]) - adblComponentSecondDerivative[0];
		}
		else
		{
			throw new RuntimeException(String.format(
			 "Invalid differentiation request for:"
			 + "%n  dlDerivativeLevel = \"%s\","
			 + "%n  mcMetricComponent = \"%s\","
			 + "%n  nIndex            = %d,"
			 + "%n  N                 = %d.",
			 dlDerivativeLevel.toString(), mcMetricComponent.toString(), nIndex, N));
		}

		return dblResult;
	}

	/**
	 * Get the specified radius and metric component of the specified level of differential of the specified index
	 * from the lists supplied.
	 * <br>
	 * The list parameter for the derivative level sought must be not <code>null</code> and must contain the same number
	 * of elements for the same radius values as any other list parameter used.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be found.
	 * @param nIndex
	 *   The zero-based index value of the metric component to be found.
	 * @param mcMetricComponent
	 *   The metric component to be found.
	 * @return
	 *   The specified radius and metric component of the specified level of differential of the specified index
	 *   from the lists supplied.
	 */
	public static Entry<Double, Double> getMetricComponentOfDerivativeLevel(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative,
	 DerivativeLevel dlDerivativeLevel, int nIndex, MetricComponent mcMetricComponent)
	{
		MetricComponents mcMetricComponents =
		 getMetricComponents(liG, liGFirstDerivative, liGSecondDerivative, dlDerivativeLevel, nIndex);
		Entry<Double, Double> entryResult = mcMetricComponents.getComponent(mcMetricComponent);
		return entryResult;
	}

	/**
	 * Set the specified metric component of the specified level of differential of the specified index
	 * using the lists supplied.
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the same number of elements
	 * for the same radius values.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param dlDerivativeLevel
	 *   The derivative level to be set.
	 * @param nIndex
	 *   The zero-based index value of the metric component to be set.
	 * @param mcMetricComponent
	 *   The metric component to be set.
	 * @param dblValue
	 *   The metric component value to be set.
	 */
	public static void setMetricComponentOfDerivativeLevel(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative,
	 DerivativeLevel dlDerivativeLevel, int nIndex, MetricComponent mcMetricComponent, double dblValue)
	{
		MetricComponents mcMetricComponents =
		 getMetricComponents(liG, liGFirstDerivative, liGSecondDerivative, dlDerivativeLevel, nIndex);
		mcMetricComponents.setComponent(mcMetricComponent, dblValue);
	}

	/**
	 * Obtain the <code>MetricComponents</code> for the given parameters.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param dlDerivativeLevel
	 *   The derivative level.
	 * @param nIndex
	 *   The zero-based index value of the metric component.
	 * @return
	 *   The <code>MetricComponents</code>.
	 */
	private static MetricComponents getMetricComponents(
	 List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative,
	 List<MetricComponents> liGSecondDerivative,
	 DerivativeLevel dlDerivativeLevel, int nIndex)
	{
		MetricComponents mcMetricComponents = null;

		switch (dlDerivativeLevel)
		{
			case None:
				mcMetricComponents = liG.get(nIndex);
				break;
			case First:
				mcMetricComponents = liGFirstDerivative.get(nIndex);
				break;
			case Second:
				mcMetricComponents = liGSecondDerivative.get(nIndex);
				break;
			default:
				throw new RuntimeException(String.format(
				 "Derivative level \"%s\" not found.", dlDerivativeLevel.toString()));
		}

		return mcMetricComponents;
	}

	/**
	 * Calculate the Jacobian matrix (values) at the given point in space-time (the radius).
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the
	 * same number of elements for the same radius values.
	 * This number of elements must be at least 5.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param nIndex
	 *   The zero-based index of the metric component of the point in space-time (the radius) to be used.
	 * @return
	 *   The Jacobian matrix (values) at the given point in space-time (the radius).
	 */
	private DoubleMatrix2D calculateJacobianMatrixValues(List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative, List<MetricComponents> liGSecondDerivative, int nIndex)
	{
		Entry<Double, Double> entry = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative,
		 None, nIndex, A);
		double dblR = entry.getKey().doubleValue();
		double dblA = entry.getValue().doubleValue();

		double dblB = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, None, nIndex, B).
		 getValue().doubleValue();

		double dAdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, A)
		 .getValue().doubleValue();

		double dBdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, B)
		 .getValue().doubleValue();

		double d2AdR2 = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, Second, nIndex, A)
		 .getValue().doubleValue();

		double dR00dA = (1 / (4.0 * dblA * dblA * dblB)) * dAdR * dAdR;
		double dR00dB = -((1 / (dblB * dblB * dblR)) * dAdR) + ((1 / (4.0 * dblA * dblB * dblB)) * dAdR * dAdR)
		 + ((1 / (2.0 * dblB * dblB * dblB)) * dAdR * dBdR) - ((1 / (2.0 * dblB * dblB)) * d2AdR2);

		double dR11dA = ((1 / (4.0 * dblA * dblA * dblB)) * dAdR * dBdR) + ((1 / (2.0 * dblA * dblA * dblA)) * dAdR * dAdR)
		 - ((1 / (2.0 * dblA * dblA)) * d2AdR2);
		double dR11dB = ((1 / (dblB * dblB * dblR)) * dBdR) + ((1 / (4.0 * dblA * dblB * dblB)) * dAdR * dBdR);

		double dR22dA = ((dblR / (2 * dblA * dblA * dblB)) * dAdR);
		double dR22dB = (1 / (dblB * dblB)) + ((dblR / (2 * dblA * dblB * dblB)) * dAdR)
		 - ((dblR / (dblB * dblB * dblB)) * dBdR);

		DoubleMatrix2D dmResult = DoubleFactory2D.dense.make(3, 2);
		dmResult.set(0, 0, dR00dA);
		dmResult.set(1, 0, dR11dA);
		dmResult.set(2, 0, dR22dA);
		dmResult.set(0, 1, dR00dB);
		dmResult.set(1, 1, dR11dB);
		dmResult.set(2, 1, dR22dB);
		return dmResult;
	}

	/**
	 * Calculate the Ricci tensor values at the given point in space-time (the radius).
	 * <br>
	 * All of the list parameters must be not <code>null</code> and contain the
	 * same number of elements for the same radius values.
	 * This number of elements must be at least 5.
	 * @param liG
	 *   A list of the metric tensor values, in order of ascending adjacent radius values.
	 * @param liGFirstDerivative
	 *   A list of first derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param liGSecondDerivative
	 *   A list of second derivative metric tensor values, in order of ascending adjacent radius values.
	 * @param nIndex
	 *   The zero-based index of the metric component of the point in space-time (the radius) to be used.
	 * @return
	 *   The Ricci tensor values at the given point in space-time (the radius) as the vector (1-D column matrix):
	 *   <code>(R00, R11, R22)T</code>.
	 */
	public static DoubleMatrix2D calculateRicciTensorValues(List<MetricComponents> liG,
	 List<MetricComponents> liGFirstDerivative, List<MetricComponents> liGSecondDerivative, int nIndex)
	{
		Entry<Double, Double> entry = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative,
		 None, nIndex, A);
		double dblR = entry.getKey().doubleValue();
		double dblA = entry.getValue().doubleValue();

		double dblB = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, None, nIndex, B).
		 getValue().doubleValue();

		double dAdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, A)
		 .getValue().doubleValue();

		double dBdR = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, First, nIndex, B)
		 .getValue().doubleValue();

		double d2AdR2 = getMetricComponentOfDerivativeLevel(liG, liGFirstDerivative, liGSecondDerivative, Second, nIndex, A)
		 .getValue().doubleValue();

		double dblR00 = ((1.0 / (dblB * dblR)) * dAdR)
		 - ((1.0 / (4.0 * dblA * dblB)) * dAdR * dAdR)
		 - ((1.0 / (4.0 * dblB * dblB)) * dAdR * dBdR)
		 + ((1.0 / (2.0 * dblB)) * d2AdR2);

		double dblR11 = -((1.0 / (dblB * dblR)) * dBdR)
		 - ((1.0 / (4.0 * dblA * dblB)) * dAdR * dBdR)
		 - ((1.0 / (4.0 * dblA * dblA)) * dAdR * dAdR)
		 + ((1.0 / (2.0 * dblA)) * d2AdR2);

		double dblR22 = -1.0 - (1.0 / dblB)
		 - ((dblR / (2.0 * dblA * dblB)) * dAdR)
		 + ((dblR / (2.0 * dblB * dblB)) * dBdR);

		DoubleMatrix2D dvResult = DoubleFactory2D.dense.make(3, 1);
		dvResult.set(0, 0, dblR00);
		dvResult.set(1, 0, dblR11);
		dvResult.set(2, 0, dblR22);
		return dvResult;
	}

	private void reportFinalTensorValues()
	{
		logger.info(String.format("The metric components (in the format \"index, r, A, B\") after the final run are:"));

		for (int i = 0; i < m_liG.size(); i++)
		{
			Entry<Double, Double> entry = getMetricComponentOfDerivativeLevel(m_liG, null, null, None, i, A);
			double dblR = entry.getKey().doubleValue();
			double dblA = entry.getValue().doubleValue();
			double dblB = getMetricComponentOfDerivativeLevel(m_liG, null, null, None, i, B).getValue().doubleValue();
			logger.info(String.format("  %3d, %,12f, %,12f, %,12f", i, dblR, dblA, dblB));
		}
	}
}
