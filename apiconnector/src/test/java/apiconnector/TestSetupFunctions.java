package apiconnector;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Run;
import org.openml.apiconnector.xml.SetupExists;
import org.openml.apiconnector.xml.SetupTag;
import org.openml.apiconnector.xml.SetupUntag;
import org.openml.apiconnector.xstream.XstreamXmlMapping;

import com.thoughtworks.xstream.XStream;

public class TestSetupFunctions {
	
	private static final String url = "https://www.openml.org/"; // Lookup test, can be done live
	private static final String session_hash = "8baa83ecddfe44b561fd3d92442e3319";
	private static final OpenmlConnector client_write = new OpenmlConnector(url,session_hash); // TODO: write account
	private static final XStream xstream = XstreamXmlMapping.getInstance();
	private static final String tag = "junittest";
	
	@Test
	public void testFindRightSetup() throws Exception {
		Integer[] run_ids = {541980, 541944};
		
		for (Integer run_id : run_ids) {
			Run r = client_write.runGet(run_id);
			int setup_id = r.getSetup_id();
			
			File description = getDescriptionFile(r, run_id);
			SetupExists se = client_write.setupExists(description);
			assertTrue(se.exists());
			assertTrue(se.getId() == setup_id);

			try {
				SetupTag st = client_write.setupTag(setup_id, tag);
				assertTrue(Arrays.asList(st.getTags()).contains(tag));
			} catch(ApiException ae) {
				// tolerate. 
				assertTrue(ae.getMessage().equals("Entity already tagged by this tag. "));
			}
			SetupUntag su = client_write.setupUntag(setup_id, tag);
			assertTrue(Arrays.asList(su.getTags()).contains(tag) == false);
		}
	}
	
	@Test
	public void testFindNonexistingSetup() throws Exception {
		Integer[] run_ids = {541980, 541944};
		Map<String,String> searchReplace = new TreeMap<String,String>();
		searchReplace.put("<oml:value>500</oml:value>", "<oml:value>bla2</oml:value>"); // matches run 541980
		searchReplace.put("<oml:value>weka.classifiers.trees.REPTree</oml:value>", "<oml:value>weka.classifiers.trees.J50 -C 0.25 -M 2</oml:value>"); // matches run 541944
		
		for (Integer run_id : run_ids) {
			Run r = client_write.runGet(run_id);
			
			File description = getDescriptionFile(r, run_id);
			
			// adjust the run file
			Path path = Paths.get(description.getAbsolutePath());
			Charset charset = StandardCharsets.UTF_8;
			
			String content = new String(Files.readAllBytes(path), charset);
			for (String search : searchReplace.keySet()) {
				content = content.replaceAll(search, searchReplace.get(search));
			}
			Files.write(path, content.getBytes(charset));
			SetupExists se = client_write.setupExists(description);
			assertTrue(se.exists() == false);
			
			// now try it with empty run file
			Run rEmpty = new Run(null, null, r.getFlow_id(), null, null, null);
			File runEmpty = Conversion.stringToTempFile(xstream.toXML(rEmpty), "openml-retest-run" + run_id, "xml");

			SetupExists se2 = client_write.setupExists(runEmpty);
			assertTrue(se2.exists() == false);
		}
	}
	
	private static File getDescriptionFile(Run r, int run_id) throws Exception {
		Integer descriptionFileId = r.getOutputFileAsMap().get("description").getFileId();
		URL descriptionUrl = client_write.getOpenmlFileUrl(descriptionFileId, "description_run" + run_id + ".xml");
		File description = File.createTempFile("description_run" + run_id, ".xml");
		FileUtils.copyURLToFile(descriptionUrl, description);
		return description;
	}
	
}
