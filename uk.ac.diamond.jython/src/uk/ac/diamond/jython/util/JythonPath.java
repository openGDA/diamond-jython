/*-
 * Copyright © 2014 Diamond Light Source Ltd.
 *
 * This file is part of GDA.
 *
 * GDA is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * GDA is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along
 * with GDA. If not, see <http://www.gnu.org/licenses/>.
 */

package uk.ac.diamond.jython.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dawb.common.util.eclipse.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JythonPath {
	
	private static Logger logger = LoggerFactory.getLogger(JythonPath.class);
	
	private static final String GIT_REPO_ENDING = ".git";
	private static final String GIT_SUFFIX = "_git";
	private static final String JYTHON_BUNDLE = "uk.ac.diamond.jython";
	private static final String JYTHON_VERSION = "2.5";
	private static final String JYTHON_BUNDLE_LOC = JYTHON_BUNDLE + ".location";
	private static String JYTHON_DIR = "jython" + JYTHON_VERSION;
	private static final String SCISOFTPY = "uk.ac.diamond.scisoft.python";
	
	/*
	 * Lists of Jars we want to/don't want to include
	 */
	private static final String[] blackListedJarDirs = {
		"uk.ac.gda.libs",
		"ch.qos.logback.eclipse",
		"ch.qos.logback.beagle",
		"org.dawb.workbench.jmx",
		GIT_REPO_ENDING,
		JYTHON_DIR
	};
	private static final String[] requiredJars = {
		"org.python.pydev",
		"cbflib-0.9",
		"org.apache.commons.codec",
		"org.apache.commons.math", // includes math3
		"uk.ac.diamond.CBFlib",
		"uk.ac.diamond.jama",
		"uk.ac.diamond.ejml",
		"uk.ac.diamond.ddogleg",
		"org.apache.commons.lang",
		"org.eclipse.dawnsci.analysis", // includes api, dataset, tree, etc
		"uk.ac.diamond.scisoft.analysis",
		"uk.ac.diamond.scisoft.diffraction.powder",
		"uk.ac.diamond.scisoft.python",
		"uk.ac.diamond.scisoft.spectroscopy",
		"uk.ac.gda.common",
		"org.eclipse.dawnsci.hdf5", // fix to http://jira.diamond.ac.uk/browse/SCI-1467
		"slf4j.api",
		"jcl.over.slf4j",
		"log4j.over.slf4j",
		"ch.qos.logback.core",
		"ch.qos.logback.classic",
		"com.springsource.org.apache.commons",
		"com.springsource.javax.media.jai.core",
		"com.springsource.javax.media.jai.codec",
		"jtransforms",
		"jai_imageio",
		"it.tidalwave.imageio.raw",
		"javax.vecmath",
		"uk.ac.diamond.org.apache.ws.commons.util",
		"uk.ac.diamond.org.apache.xmlrpc.client",
		"uk.ac.diamond.org.apache.xmlrpc.common",
		"uk.ac.diamond.org.apache.xmlrpc.server",
		"com.thoughtworks.xstream",
		"uk.ac.diamond.org.jscience4",
	};
	/*
	 * Plugins we want/don't want
	 */
	private final static String[] pluginKeys = {
		"org.eclipse.dawnsci.hdf5", // required for loading to work in client started from IDE
		"org.eclipse.dawnsci.analysis", // includes api, dataset, tree, etc
		"uk.ac.diamond.scisoft.analysis",
		"uk.ac.diamond.scisoft.diffraction.powder",
		"uk.ac.diamond.scisoft.python",
		"uk.ac.diamond.CBFlib",
		"uk.ac.gda.common",
		"ncsa.hdf"
	};
	private static String[] extraPlugins = null;

	
	/**
	 * Provides location of plugin files; behaviour depends whether we're running in eclipse
	 * @param isRunningInEclipse Boolean, true if running in eclipse
	 * @return Directory where plugins live (defined as parent of current bundle)
	 */
	public static File getPluginsDirectory(boolean isRunningInEclipse) {
		try {
			File scisoftParent = BundleUtils.getBundleLocation(SCISOFTPY).getParentFile();
			if (isRunningInEclipse) {
				scisoftParent = scisoftParent.getParentFile();
			}
			//Need to include a logging statement
			return scisoftParent;
		} catch (Exception e) {
			logger.error("Could not find Scisoft Python plugin", e);
		}
		return null;
	}
	
	/**
	 * Gets the interpreter directory using the bundle location
	 * @return directory path 
	 * @throws Exception
	 */
	public static File getInterpreterDirectory() throws Exception {
		File jyBundleLoc = null;
		try {
			jyBundleLoc = BundleUtils.getBundleLocation(JYTHON_BUNDLE);
		} catch (Exception ignored) {
		}
		if (jyBundleLoc == null) {
			if (System.getProperty(JYTHON_BUNDLE_LOC)==null)
				throw new Exception("Please set the property '" + JYTHON_BUNDLE_LOC + "' for this test to work!");
			jyBundleLoc = new File(System.getProperty(JYTHON_BUNDLE_LOC));
		}
		jyBundleLoc = new File(jyBundleLoc, JYTHON_DIR);
		logger.info("Jython bundle found at: {}", jyBundleLoc);
		return jyBundleLoc;
	}
	
	/**
	 * Recursively search through a given directory to locate all jar files provided 
	 * they are in the requiredJars/extraPlugins lists
	 * @param directory location searched for jar files 
	 * @return List of jar files which will be added to the path
	 */
	public static final List<File> findJars(File directory) {
		final List<File> libs = new ArrayList<File>();
	
		if (directory.isDirectory()) {
			for (File f : directory.listFiles()) {
				final String name = f.getName();
				// if the file is a jar, then add it
				if (name.endsWith(".jar")) {
					if (isRequired(f, requiredJars, extraPlugins)) {
						libs.add(f);
					}
				} else if (f.isDirectory() && !isRequired(f, blackListedJarDirs)) {
					libs.addAll(findJars(f));
				}
			}
		}
	
		return libs;
	}
	
	/**
	 * Method returns path to plugin directories (behaviour depends on whether in eclipse
	 * @param directory Search location
	 * @param isRunningInEclipse Boolean, true if running in eclipse
	 * @return list of directories
	 */
	public static List<File> findDirs(File directory, boolean isRunningInEclipse) {
		//TODO Simplify this method to remove old git & recycle code
		final List<File> plugins = new ArrayList<File>();
		
		if (isRunningInEclipse) {
			// get down to the git checkouts
			// only do this if we are running inside Eclipse
			List<File> dirs = new ArrayList<File>();
			
			//Look for subdirectories ending with the GIT_REPO_ENDING or equal to 'scisoft'
			for (File d : directory.listFiles()) {
				if (d.isDirectory()) {
					String n = d.getName();
					if (n.endsWith(GIT_REPO_ENDING)) {
						dirs.add(d);
						// Old source layout
						//Can this be removed? (06-11-2014)
					} else if (n.equals("scisoft")) {
						for (File f : d.listFiles()) {
							if (f.isDirectory()) {
								if (f.getName().endsWith(GIT_REPO_ENDING)) {
									logger.debug("Adding scisoft directory {}", f);
									dirs.add(f);
								}
							}
						}
					}
				}
			}
			//Look inside sub-directories for sub-directories which are in the plugins lists
			for (File f : dirs) {
				for (File p : f.listFiles()) {
					if (p.isDirectory()) {
						if (isRequired(p, pluginKeys, extraPlugins)) {
							logger.debug("Adding plugin directory {}", p);
							plugins.add(p);
						}
					}
				}
			}
		} else {
			// Look inside directory for sub-directories which are in the plugins lists
			if (directory.isDirectory()) {
				for (File f : directory.listFiles()) {
					if (f.isDirectory()) {
						if (isRequired(f, pluginKeys, extraPlugins)) {
							logger.debug("Adding plugin directory {}", f);
							plugins.add(f);
						}
					}
				}
			}
		}
		// Return all the directories we found
		return plugins;
	}
	
	/**
	 * Check whether a file is in a list
	 * @param file Filename to search for
	 * @param keys List to search against
	 * @return Boolean, true if file is in list
	 */
	private static boolean isRequired(File file, String[] keys) {
		return isRequired(file, keys, null);
	}
	private static boolean isRequired(File file, String[] keys, String[] extraKeys) {
		String filename = file.getName();
//		logger.debug("Jar/dir found: {}", filename);
		for (String key : keys) {
			if (filename.startsWith(key)) return true;
		}
		if (extraKeys != null) {
			for (String key : extraKeys) {
				if (filename.startsWith(key)) return true;
			}
		}
		return false;
	}

}
