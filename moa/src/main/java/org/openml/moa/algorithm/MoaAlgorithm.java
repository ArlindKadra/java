package org.openml.moa.algorithm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.FlowExists;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.Run.Parameter_setting;
import org.openml.apiconnector.xml.UploadFlow;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.openml.moa.settings.MoaSettings;

import weka.core.Utils;
import moa.classifiers.Classifier;
import moa.options.ClassOption;
import moa.options.FileOption;
import moa.options.FlagOption;
import moa.options.Option;
import moa.options.WEKAClassOption;

public class MoaAlgorithm {
	
	public static int getFlowId( Flow implementation, Classifier classifier, OpenmlConnector apiconnector ) throws Exception {
		try {
			// First ask OpenML whether this implementation already exists
			FlowExists result = apiconnector.flowExists( implementation.getName(), implementation.getExternal_version() );
			
			if(result.exists()) return result.getId();
		} catch( Exception e ) { /* Suppress Exception since it is totally OK.*/ }
		
		// It does not exist. Create it. 
		String xml = XstreamXmlMapping.getInstance().toXML( implementation );
		//System.err.println(xml);
		File implementationFile = Conversion.stringToTempFile( xml, implementation.getName(), "xml");
		File source = null;
		File binary = null;
		try { source = getFile( classifier, "src/", "java" ); } catch(IOException e) {}
		try { binary = getFile( classifier, "bin/", "class" ); } catch(IOException e) {}
		UploadFlow ui = apiconnector.flowUpload(implementationFile, binary, source);
		return ui.getId();
	}
	
	public static ArrayList<Run.Parameter_setting> getOptions( Flow i, Option[] options ) {
		ArrayList<Run.Parameter_setting> result = new ArrayList<Run.Parameter_setting>();
		for( Option option : options ) {
			if( option instanceof FlagOption ) {
				FlagOption o = (FlagOption) option;
				result.add( new Parameter_setting(i.getId(), o.getCLIChar() + "", o.isSet() ? "true" : "false") );
			} else if( option instanceof FileOption ) {
				// ignore file options
				continue;
			} else if( option instanceof ClassOption ) {
				ClassOption o = (ClassOption) option;
				if( o.getRequiredType().isAssignableFrom( Classifier.class ) ) {
					try {
						Classifier subclassifier = (Classifier) ClassOption.cliStringToObject( o.getValueAsCLIString(), o.getRequiredType(), null );
						Flow subimplementation = create( subclassifier );
						
						result.addAll( getOptions( i.getComponentByName( subimplementation.getName() ), subclassifier.getOptions().getOptionArray() ) );
						result.add( new Parameter_setting( i.getId(), option.getCLIChar() + "", subclassifier.getClass().getName() ) );
					} catch (Exception e) {
						result.add( new Parameter_setting(i.getId(), option.getCLIChar() + "", option.getValueAsCLIString() ) );
						e.printStackTrace(); 
					}
				} else {
					result.add( new Parameter_setting(i.getId(), option.getCLIChar() + "", option.getValueAsCLIString() ) );
				}
			} else if( option instanceof WEKAClassOption ) {
				try {
					String[] params = Utils.splitOptions( option.getValueAsCLIString() );
					Flow subimplementation = wekaSubimplementation( (WEKAClassOption) option );
					result.addAll( WekaAlgorithm.getParameterSetting( params, i.getComponentByName( subimplementation.getName() ) ) );
					result.add( new Parameter_setting( i.getId(), option.getCLIChar() + "", params[0] ) );
				} catch( Exception e ) {
					result.add( new Parameter_setting(i.getId(), option.getCLIChar() + "", option.getValueAsCLIString() ) );
					e.printStackTrace(); 
				}
			}else {
				result.add( new Parameter_setting(i.getId(), option.getCLIChar() + "", option.getValueAsCLIString() ) );
			}
		}
		
		return result;
	}
	
	public static Flow create( Classifier classifier ) {
		String classPath = classifier.getClass().getName();
		String classifierName = classPath.substring( classPath.lastIndexOf('.') + 1 );
		String name = "moa." + classifierName;
		String version = "1.0"; //TODO: MOA does not support retrieval of version?
		String description = "Moa implementation of " + classifierName;
		String language = "English";
		String dependencies = MoaSettings.MOA_VERSION; // TODO: No version information?
		
		Flow i = new Flow( name, dependencies + "_" + version, description, language, dependencies );
		for( Option option : classifier.getOptions().getOptionArray() ) {
			if( option instanceof FlagOption ) {
				FlagOption fo = (FlagOption) option;
				i.addParameter( fo.getCLIChar() + "", "flag", "false", fo.getName() + ": " + fo.getPurpose() );
			} else if( option instanceof ClassOption ) {
				ClassOption co = (ClassOption) option;
				i.addParameter(co.getCLIChar() + "", "baselearner", co.getDefaultCLIString(), co.getName() + ": " + co.getPurpose() );
				
				if( co.getRequiredType().isAssignableFrom( Classifier.class ) ) {
					try {
						Flow subimplementation = create( (Classifier) ClassOption.cliStringToObject( co.getValueAsCLIString(), co.getRequiredType(), null ) );
						i.addComponent(co.getCLIChar() + "", subimplementation );
					} catch (Exception e) {	e.printStackTrace(); }
				}
			} else if( option instanceof WEKAClassOption ) {
				WEKAClassOption wco = (WEKAClassOption) option;
				i.addParameter(wco.getCLIChar() + "", "baselearner", wco.getDefaultCLIString(), wco.getName() + ": " + wco.getPurpose() );
				
				try {
					i.addComponent(wco.getCLIChar() + "", wekaSubimplementation(wco) );
				} catch(Exception e) { e.printStackTrace(); }
			} else {
				i.addParameter( option.getCLIChar() + "", "option", option.getDefaultCLIString(), option.getName() + ": " + option.getPurpose() );
			}
		}
		
		return i;
	}
	
	public static File getFile( Classifier classifier, String prefix, String extension ) throws IOException {
		Class<? extends Classifier> c = classifier.getClass();
		String sourcefile = c.getName().replace( '.', '/' );
		InputStream is = getFis( sourcefile + "." + extension, prefix );
		if( is == null ) throw new IOException( "Could not find resource " + sourcefile + "." + extension );
		BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
		StringBuilder totalSource = new StringBuilder();
		String line = br.readLine();
		while( line != null ) {
			totalSource.append( line + "\n" );
			line = br.readLine();
		}
		return Conversion.stringToTempFile(totalSource.toString(), c.getName(), extension);
	}
	
	private static InputStream getFis( String classname, String prefix ) {
		WekaAlgorithm loader = new WekaAlgorithm();
		InputStream is = null;
		
		is = loader.getClass().getResourceAsStream('/'+classname);
		
		if( is == null ) {
			try {
				File f = new File( prefix + classname );
				if( f.exists() ) {
					is = new FileInputStream( f );
				}
			} catch( IOException e ) { e.printStackTrace(); }
		}
		return is;
	}
	
	private static Flow wekaSubimplementation( WEKAClassOption wco ) throws Exception {
		if( wco.getRequiredType().isAssignableFrom( weka.classifiers.Classifier.class ) ) {
			String weka_identifier = wco.getValueAsCLIString();
			String weka_classifier = weka_identifier.substring(0, weka_identifier.indexOf(' '));
			String weka_parameters = weka_identifier.substring(weka_identifier.indexOf(' ')+1);
			return WekaAlgorithm.create( weka_classifier, weka_parameters );
		} else throw new Exception("Option required type not assignable from weka.classifiers.Classifiers.class: " + wco.getRequiredType().getName() );
	}
}