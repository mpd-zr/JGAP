/*
 * This file is part of JGAP.
 *
 * JGAP offers a dual license model containing the LGPL as well as the MPL.
 *
 * For licensing information please see the file license.txt included with JGAP
 * or have a look at the top of class org.jgap.Chromosome which representatively
 * includes the JGAP license policy applicable for any file delivered with JGAP.
 */
package org.jgap.gp;

import org.jgap.*;
import org.jgap.gp.impl.*;
import org.simpleframework.xml.Default;

@Default
/**
 * Base class for GPProgram's. See org.jgap.gp.impl.GPProgram for an
 * implementation.
 *
 * @author Klaus Meffert
 * @since 3.0
 */
public abstract class GPProgramBase
implements IGPProgram {
	
	/** String containing the CVS revision. Read out via reflection!*/
	private final static String CVS_REVISION = "$Revision: 1.14 $";

	private double m_fitnessValue = FitnessFunction.NO_FITNESS_VALUE;

	private transient GPConfiguration m_conf;

	private boolean reEvaluate;

	/**
	 * The scaling factor is used for the parsimony pressure to rescale the fitness value based 
	 * on its current performance within the generation. By default the value is 1 ad after cloning it is also reset to 1
	 */
	private double m_scalingFactor = 1d;


	/**
	 * Return type per chromosome.
	 */
	private String[] m_types;

	/**
	 * Argument types for ADF's
	 */
	private String[][] m_argTypes;

	/**
	 * Available GP-functions.
	 */
	private CommandGene[][] m_nodeSets;

	/**
	 * Minimum depth per each chromosome
	 */
	private int[] m_minDepths;

	/**
	 * Maximum depth per each chromosome
	 */
	private int[] m_maxDepths;

	/**
	 * Maximum number of nodes allowed per chromosome (when exceeded program
	 * aborts)
	 */
	private int m_maxNodes;

	/**
	 * Free to use data object.
	 */
	private transient Object m_applicationData;

	/**
	 * Default constructor, only for dynamic instantiation.
	 *
	 * @throws Exception
	 *
	 * @author Klaus Meffert
	 * @since 3.3.4
	 */
	public GPProgramBase()
			throws Exception {
	}

	public GPProgramBase(GPConfiguration a_conf)
			throws InvalidConfigurationException {
		if (a_conf == null) {
			throw new InvalidConfigurationException("Configuration must not be null!");
		}
		m_conf = a_conf;
	}

	public GPProgramBase(IGPProgram a_prog)
			throws InvalidConfigurationException {
		this(a_prog.getGPConfiguration());
		setTypes(a_prog.getTypes());
		setArgTypes(a_prog.getArgTypes());
		m_nodeSets = a_prog.getNodeSets();
		m_maxDepths = a_prog.getMaxDepths();
		m_minDepths = a_prog.getMinDepths();
		m_maxNodes = a_prog.getMaxNodes();
	}

	public GPConfiguration getGPConfiguration() {
		return m_conf;
	}

	/**
	 * Compares this entity against the specified object.
	 *
	 * @param a_other the object to compare against
	 * @return true: if the objects are the same, false otherwise
	 *
	 * @author Klaus Meffert
	 * @since 3.0
	 */
	public boolean equals(Object a_other) {
		try {
			return compareTo(a_other) == 0;
		} catch (ClassCastException cex) {
			return false;
		}
	}

	/**
	 * @return fitness value of this program determined via the registered
	 * fitness function
	 *
	 * @author Klaus Meffert
	 * @since 3.0
	 */
	public double calcFitnessValue() {
		GPFitnessFunction normalFitnessFunction = getGPConfiguration().
				getGPFitnessFunction();
		if (normalFitnessFunction != null) {
			// Grab the "normal" fitness function and ask it to calculate our
			// fitness value.
			// --------------------------------------------------------------
			m_fitnessValue = normalFitnessFunction.getFitnessValue(this);
		}
		if (Double.isInfinite(m_fitnessValue)) {
			return GPFitnessFunction.NO_FITNESS_VALUE;
		}
		else {
			return m_fitnessValue;
		}
	}

	/**
	 * @return fitness value of this program, cached access
	 *
	 * @author Klaus Meffert
	 * @since 3.0
	 */
	public double getFitnessValue() {
		if (m_fitnessValue >= 0.000d && !reEvaluate) {
			return m_scalingFactor*m_fitnessValue;
		}
		else {
			return m_scalingFactor * calcFitnessValue();
		}
	}

	/**
	 * @return computed fitness value of this program, may be unitialized
	 *
	 * @author Klaus Meffert
	 * @since 3.2
	 */
	public double getFitnessValueDirectly() {
		return m_fitnessValue;
	}

	public void setFitnessValue(double a_fitness) {
		m_fitnessValue = a_fitness;
	}

	@Override
	public void setScalingFactor(double scale) {
		m_scalingFactor = scale;
	}

	public void setTypes(Class[] a_types) {
		String[] types = new String[a_types.length];

		for (int i = 0; i<a_types.length;i++)
			types[i] = a_types[i].getName();

		m_types = types;
	}

	public Class[] getTypes() {
		Class[] types = new Class[m_types.length];

		for (int i = 0; i<m_types.length;i++)
			types[i] = getType(i);

		return types;
	}

	public Class getType(int a_index) {
		try {
			return Class.forName(m_types[a_index]);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return Void.class;
	}

	public void setArgTypes(Class[][] a_argTypes) {
		String[][] types = new String[a_argTypes.length][];

		for (int i = 0; i<a_argTypes.length;i++) {
			types[i] = new String[a_argTypes[i].length];
			for (int j = 0; j < a_argTypes[i].length; j++)
				types[i][j] = a_argTypes[i][j].getName();
		}

		m_argTypes = types;
	}

	public Class[][] getArgTypes() {
		Class[][] types = new Class[m_argTypes.length][];

		for (int i = 0; i<m_argTypes.length;i++) {
			types[i] = new Class[m_argTypes[i].length];
			for (int j = 0; j < m_argTypes[i].length; j++)
				try {
					types[i][j] = Class.forName(m_argTypes[i][j]);
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
		}

		return types;
	}

	public Class[] getArgType(int a_index) {
		Class [] types = new Class[m_argTypes[a_index].length];

		for (int i = 0; i < m_argTypes[a_index].length; i++)
			try {
				types[i] = Class.forName(m_argTypes[a_index][i]);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

		return types;
	}

	public void setNodeSets(CommandGene[][] a_nodeSets) {
		m_nodeSets = a_nodeSets;
	}

	public CommandGene[][] getNodeSets() {
		return m_nodeSets;
	}

	public CommandGene[] getNodeSet(int a_index) {
		return m_nodeSets[a_index];
	}

	public void setMaxDepths(int[] a_maxDepths) {
		m_maxDepths = a_maxDepths;
	}

	public int[] getMaxDepths() {
		return m_maxDepths;
	}

	public void setMinDepths(int[] a_minDepths) {
		m_minDepths = a_minDepths;
	}

	public int[] getMinDepths() {
		return m_minDepths;
	}

	public void setMaxNodes(int a_maxNodes) {
		m_maxNodes = a_maxNodes;
	}

	public int getMaxNodes() {
		return m_maxNodes;
	}

	/**
	 * Sets the application data object.
	 *
	 * @param a_data the object to set
	 *
	 * @author Klaus Meffert
	 * @since 3.01
	 */
	public void setApplicationData(Object a_data) {
		m_applicationData = a_data;
	}

	/**
	 * @return the application data object set
	 *
	 * @author Klaus Meffert
	 * @since 3.01
	 */
	public Object getApplicationData() {
		return m_applicationData;
	}

	/**
	 * @return deep clone of this instance
	 *
	 * @author Klaus Meffert
	 * @since 3.2
	 */
	public abstract Object clone();
}
