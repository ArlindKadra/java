/*
 *  OpenmlApiConnector - Java integration of the OpenML Web API
 *  Copyright (C) 2014 
 *  @author Jan N. van Rijn (j.n.van.rijn@liacs.leidenuniv.nl)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 */
package apiconnector;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.junit.Test;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.ApiException;
import org.openml.apiconnector.io.HttpConnector;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.xml.Data;
import org.openml.apiconnector.xml.DataDelete;
import org.openml.apiconnector.xml.DataFeature;
import org.openml.apiconnector.xml.DataQuality;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.DataTag;
import org.openml.apiconnector.xml.DataUntag;
import org.openml.apiconnector.xml.TaskDelete;
import org.openml.apiconnector.xml.TaskTag;
import org.openml.apiconnector.xml.TaskUntag;
import org.openml.apiconnector.xml.Task_new;
import org.openml.apiconnector.xml.UploadDataSet;
import org.openml.apiconnector.xml.UploadTask;
import org.openml.apiconnector.xml.Data.DataSet;
import org.openml.apiconnector.xml.Task_new.Input;
import org.openml.apiconnector.xstream.XstreamXmlMapping;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.thoughtworks.xstream.XStream;

public class TestDataFunctionality {
	private static final String data_file = "data/iris.arff";
	private static final int probe = 61;
	private static final String tag = "junittest";

	private static final String url = "http://capa.win.tue.nl/";
	private static final String session_hash = "d488d8afd93b32331cf6ea9d7003d4c3";
	private static final OpenmlConnector client = new OpenmlConnector(url, session_hash);
	private static final XStream xstream = XstreamXmlMapping.getInstance();

	@Test
	public void testApiDataDownload() {
		try {
			DataSetDescription dsd = client.dataGet(probe);
			DataFeature features = client.dataFeatures(probe);
			DataQuality qualities = client.dataQualities(probe);
			
			File tempDsd = Conversion.stringToTempFile(xstream.toXML(dsd), "data", "xml");
			File tempXsd = client.getXSD("openml.data.upload");
			
			String url = client.getApiUrl() + "data/" + probe;
			String raw = HttpConnector.getStringFromUrl(url + "?api_key=" + client.getApiKey(), false);
			
			assertTrue(Conversion.validateXML(tempDsd, tempXsd));
			
			
			String dsdFromOpenml = toPrettyString(raw, 0);
			String dsdFromConnector = toPrettyString(xstream.toXML(dsd), 0);
			
			assertTrue(dsdFromOpenml.equals(dsdFromConnector));
			
			// very easy checks, should all pass
			assertTrue( dsd.getId() == probe );
			assertTrue( features.getFeatures().length > 0 );
			assertTrue( qualities.getQualities().length > 0 );
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	public void testApiUploadDownload() {
		try {
			DataSetDescription dsd = new DataSetDescription("test", "Unit test should be deleted", "arff", "class");
			String dsdXML = xstream.toXML(dsd);
			File description = Conversion.stringToTempFile(dsdXML, "test-data", "arff");
			UploadDataSet ud = client.dataUpload(description, new File(data_file));
			DataTag dt = client.dataTag(ud.getId(), tag);
			assertTrue(Arrays.asList(dt.getTags()).contains(tag));
			
			
			// create task upon it
			Input estimation_procedure = new Input("estimation_procedure", "1");
			Input data_set = new Input("source_data", "" + ud.getId());
			Input target_feature = new Input("target_feature", "class");
			Input[] inputs = {estimation_procedure, data_set, target_feature};
			UploadTask ut = client.taskUpload(inputsToTaskFile(inputs, 1));
			
			TaskTag tt = client.taskTag(ut.getId(), tag);
			assertTrue(Arrays.asList(tt.getTags()).contains(tag));
			TaskUntag tu = client.taskUntag(ut.getId(), tag);
			assertTrue(tu.getTags() == null);
			
			try {
				client.dataDelete(ud.getId());
				// this SHOULD fail, we should not be allowed to delete data that contains tasks.
				fail("Problem with API. Dataset ("+ud.getId()+") was deleted while it contains a task ("+ut.getId()+"). ");
			} catch(ApiException ae) {}
			
			
			// delete the task
			TaskDelete td = client.taskDelete(ut.getId());
			assertTrue(td.get_id().equals(ut.getId()));
			
			// and delete the data
			DataUntag du = client.dataUntag(ud.getId(), tag);
			assertTrue(du.getTags() == null);
			
			DataDelete dd = client.dataDelete(ud.getId());
			assertTrue(ud.getId() == dd.get_id());
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	public void testApiDataList() {
		try {
			Data datasets = client.dataList("study_1");
			assertTrue(datasets.getData().length > 20);
			for (DataSet dataset : datasets.getData()) {
				assertTrue(dataset.getQualities().length > 5);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("Test failed: " + e.getMessage());
		}
	}

	@Test
	public void testApiAdditional() {
		try {
			client.dataQualitiesList();

		} catch (Exception e) {
			e.printStackTrace();
			fail("Test failed: " + e.getMessage());
		}
	}
	
	// function that formats xml consistently, making it easy to compare them. 
	public static String toPrettyString(String xml, int indent) {
	    try {
	        // Turn xml string into a document
	        Document document = DocumentBuilderFactory.newInstance()
	                .newDocumentBuilder()
	                .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

	        // Remove whitespaces outside tags
	        XPath xPath = XPathFactory.newInstance().newXPath();
	        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
	                                                      document,
	                                                      XPathConstants.NODESET);

	        for (int i = 0; i < nodeList.getLength(); ++i) {
	            Node node = nodeList.item(i);
	            node.getParentNode().removeChild(node);
	        }

	        // Setup pretty print options
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	        // Return pretty print xml string
	        StringWriter stringWriter = new StringWriter();
	        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
	        return stringWriter.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
	public static File inputsToTaskFile(Input[] inputs, int ttid) throws IOException {
		Task_new task = new Task_new(null, ttid, inputs, null);
		File taskFile = Conversion.stringToTempFile(xstream.toXML(task), "task", "xml");
		return taskFile;
	}
}
