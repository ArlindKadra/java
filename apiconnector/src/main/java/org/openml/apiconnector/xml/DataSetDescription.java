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
package org.openml.apiconnector.xml;

import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.openml.apiconnector.algorithms.ArffHelper;
import org.openml.apiconnector.settings.Constants;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

@XStreamAlias("oml:data_set_description")
public class DataSetDescription implements Serializable {
	private static final long serialVersionUID = 987612341129L;

	@XStreamAsAttribute
	@XStreamAlias("xmlns:oml")
	private final String oml = Constants.OPENML_XMLNS;

	@XStreamAlias("oml:id")
	private Integer id;

	@XStreamAlias("oml:name")
	private String name;

	@XStreamAlias("oml:version")
	private String version;

	@XStreamAlias("oml:description")
	private String description;

	@XStreamAlias("oml:format")
	private String format;

	@XStreamImplicit(itemFieldName = "oml:creator")
	private String[] creator;

	@XStreamImplicit(itemFieldName = "oml:contributor")
	private String[] contributor;

	@XStreamAlias("oml:collection_date")
	private String collection_date;

	@XStreamAlias("oml:upload_date")
	private String upload_date;

	@XStreamAlias("oml:language")
	private String language;

	@XStreamAlias("oml:licence")
	private String licence;

	@XStreamAlias("oml:url")
	private String url;

	@XStreamAlias("oml:file_id")
	private Integer file_id;

	@XStreamAlias("oml:default_target_attribute")
	private String default_target_attribute;

	@XStreamAlias("oml:row_id_attribute")
	private String row_id_attribute;

	@XStreamImplicit(itemFieldName = "ignore_attribute")
	private String[] ignore_attribute;

	@XStreamAlias("oml:version_label")
	private String version_label;

	@XStreamImplicit(itemFieldName = "oml:tag")
	private String[] tag;

	@XStreamAlias("oml:visibility")
	private String visibility;

	@XStreamAlias("oml:original_data_url")
	private String original_data_url;

	@XStreamAlias("oml:paper_url")
	private String paper_url;

	@XStreamAlias("oml:status")
	private String status;

	@XStreamAlias("oml:md5_checksum")
	private String md5_checksum;

	// do not serialize
	@XStreamOmitField
	private File dataset_cache;

	/*
	 * Constructor used from the Register Dataset Dialog. Set "null" for
	 * unspecified values that are optional.
	 */

	public DataSetDescription(Integer id, String name, String version, String description, String[] creator,
			String[] contributor, String format, String collection_date, String language, String licence, String url,
			String row_id_attribute, String default_target_attribute, String[] ignore_attribute, String[] tag,
			String md5_checksum) {
		this.id = id;
		this.name = name;
		this.version = version;
		this.description = description;
		this.creator = creator;
		this.contributor = contributor;
		this.format = format;
		this.collection_date = collection_date;
		this.language = language;
		this.upload_date = null;
		this.licence = licence;
		this.url = url;
		this.row_id_attribute = row_id_attribute;
		this.default_target_attribute = default_target_attribute;
		this.ignore_attribute = ignore_attribute;
		this.tag = tag;
		this.md5_checksum = md5_checksum;
	}

	public DataSetDescription(String name, String description, String format, String default_target_attribute) {
		this.id = null;
		this.name = name;
		this.version = null;
		this.description = description;
		this.creator = null;
		this.contributor = null;
		this.format = format;
		this.collection_date = null;
		this.language = null;
		this.upload_date = null;
		this.licence = null;
		this.url = null;
		this.row_id_attribute = null;
		this.default_target_attribute = default_target_attribute;
		this.md5_checksum = null;
	}

	public DataSetDescription(String name, String description, String format, String url,
			String default_target_attribute) {
		this.id = null;
		this.name = name;
		this.version = null;
		this.description = description;
		this.creator = null;
		this.contributor = null;
		this.format = format;
		this.collection_date = null;
		this.language = null;
		this.upload_date = null;
		this.licence = null;
		this.url = url;
		this.row_id_attribute = null;
		this.default_target_attribute = default_target_attribute;
		this.md5_checksum = null;
	}

	public String getOml() {
		return oml;
	}

	public Integer getId() {
		return id;
	}

	public void unsetId() {
		id = null;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public String getDescription() {
		return description;
	}

	public String getFormat() {
		return format;
	}

	public String[] getCreator() {
		return creator;
	}

	public String[] getContributor() {
		return contributor;
	}

	public String getCollection_date() {
		return collection_date;
	}

	public String getUpload_date() {
		return upload_date;
	}

	public String getLanguage() {
		return language;
	}

	public String getLicence() {
		return licence;
	}

	public String getUrl() {
		return url;
	}

	public void unsetUrl() {
		this.url = null;
	}

	public Integer getFile_id() {
		return file_id;
	}

	public String getRow_id_attribute() {
		return row_id_attribute;
	}

	public String getDefault_target_attribute() {
		return default_target_attribute;
	}

	public String getVersion_label() {
		return version_label;
	}

	public String[] getIgnore_attribute() {
		return ignore_attribute;
	}

	public String[] getTag() {
		return tag;
	}

	public void addTag(String new_tag) {
		// check if tag is not already present
		if (tag != null) {
			if (Arrays.asList(tag).contains(new_tag) == true) {
				return;
			}
		}
		tag = ArrayUtils.addAll(tag, new_tag);
	}

	public String getVisibility() {
		return visibility;
	}

	public String getMd5_checksum() {
		return md5_checksum;
	}

	public File getDataset(String api_key) throws Exception {
		// for privacy settings
		String url_suffix = "";
		if (api_key != null) {
			url_suffix = "?api_key=" + api_key;
		}

		if (dataset_cache == null) {
			dataset_cache = ArffHelper.downloadAndCache("dataset", getId(), getFormat(), new URL(getUrl() + url_suffix), getMd5_checksum());
		}
		return dataset_cache;
	}
}