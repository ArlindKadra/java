package apiconnector;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Arrays;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Flow;
import org.openml.apiconnector.xml.FlowDelete;
import org.openml.apiconnector.xml.FlowTag;
import org.openml.apiconnector.xml.FlowUntag;
import org.openml.apiconnector.xml.UploadFlow;
import org.openml.apiconnector.xstream.XstreamXmlMapping;

import com.thoughtworks.xstream.XStream;

public class TestFlowFunctionality {
	private static final int probe = 10;

	private static final String url = "https://test.openml.org/";
	private static final OpenmlConnector client_write = new OpenmlConnector(url, "8baa83ecddfe44b561fd3d92442e3319");
	private static final OpenmlConnector client_read = new OpenmlConnector(url, "c1994bdb7ecb3c6f3c8f3b35f4b47f1f");
	private static final XStream xstream = XstreamXmlMapping.getInstance();
	private static final String tag = "junittest";
	
	@Test
	public void testApiFlowDownload() throws Exception {
		Flow flow = client_read.flowGet(probe);

		File tempXml = Conversion.stringToTempFile(xstream.toXML(flow), "flow", "xml");
		File tempXsd = client_read.getXSD("openml.implementation.upload");

		assertTrue(Conversion.validateXML(tempXml, tempXsd));

		// very easy checks, should all pass
		assertTrue(flow.getId() == probe);

	}
	
	@Test
	public void testApiFlowUpload() throws Exception {
		try {
			Flow created = new Flow("test2", "weka.classifiers.test.janistesting", "test", "test should be deleted", "english", "UnitTest");
			created.addComponent("B", new Flow("test2", "weka.classifiers.test.janistesting.subflow", "test2", "test should be deleted", "english", "UnitTest") );
			String flowXML = xstream.toXML(created);
			
			System.out.println(flowXML);
			
			File f = Conversion.stringToTempFile(flowXML, "test", "xml");
			
			UploadFlow uf = client_write.flowUpload(f, f, f);
			
			FlowTag ft = client_write.flowTag(uf.getId(), tag);
			assertTrue(Arrays.asList(ft.getTags()).contains(tag));
			FlowUntag fu = client_write.flowUntag(uf.getId(), tag);
			assertTrue(fu.getTags() == null);
			
			FlowDelete fd = client_write.flowDelete(uf.getId());
			System.out.println(fd.getId());
			
		} catch (Exception e) {
			e.printStackTrace();
			// possibly the previous test failed, and left the exact same flow on the server.
			if (e instanceof ApiException) {
				ApiException apiException = (ApiException) e;
				if (apiException.getCode() == 171) {
					int index = apiException.getMessage().indexOf("implementation_id") + "implementation_id".length()+1;
					Integer implementation_id = Integer.parseInt(apiException.getMessage().substring(index));
					// delete it
					client_write.flowDelete(implementation_id);
					// and try again
					testApiFlowUpload();
					
				} else {
					fail("Test failed: " + e.getMessage());
				}
			}
			
		}
	}

	@Test
	public void testApiFlowUploadDuplicate() throws Exception {
		Flow created = new Flow("test2", "weka.classifiers.test.javaunittest", "test", "test should be deleted",
				"english", "UnitTest");
		created.addComponent("B", new Flow("test2", "weka.classifiers.test.janistesting", "test2",
				"test should be deleted", "english", "UnitTest"));
		created.setCustom_name("Jans flow");
		String flowXML = xstream.toXML(created);

		File f = Conversion.stringToTempFile(flowXML, "test", "xml");

		UploadFlow uf = client_write.flowUpload(f, f, f);

		try {
			UploadFlow uf2 = client_write.flowUpload(f, null, null);
			System.out.println(uf2.getId());

			fail("Test failed, flow upload should have been blocked.");
		} catch (Exception e) {
			// we expect an exception
			System.out.println(e.getMessage());
		}
		client_write.flowDelete(uf.getId());

	}

	@Test
	public void testUploadComplicatedFlow() throws Exception {
		Random random = new Random(System.currentTimeMillis());
		String complicatedFlow = FileUtils.readFileToString(new File("data/FilteredClassifier_RandomForest.xml"))
				.replace("{SENTINEL}", "SEN" + Math.abs(random.nextInt()));
		File f = Conversion.stringToTempFile(complicatedFlow, "test", "xml");
		
		UploadFlow uf = client_write.flowUpload(f, null, null);
		client_write.flowDelete(uf.getId());
	}
}
