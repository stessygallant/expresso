package com.sgitmanagement.expressoext.networkfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.sgitmanagement.expresso.util.Util;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
public class NetworkFile {
	private String path;
	private String name;
	private String format;
	private boolean directory;
	private boolean image;
	private List<NetworkFile> networkFiles;

	public NetworkFile() {

	}

	public NetworkFile(String parentPath, String name, boolean directory) {
		super();
		this.path = (Util.nullifyIfNeeded(parentPath) == null ? "" : parentPath + File.separator) + name;
		this.directory = directory;
		this.name = name;

		if (!directory && name.indexOf('.') != -1) {
			this.format = name.substring(name.lastIndexOf('.') + 1);
		}
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public List<NetworkFile> getNetworkFiles() {
		return networkFiles;
	}

	public void setNetworkFiles(List<NetworkFile> networkFiles) {
		this.networkFiles = networkFiles;
	}

	public void addNetworkFile(NetworkFile networkFile) {
		if (this.networkFiles == null) {
			this.networkFiles = new ArrayList<>();
		}
		this.networkFiles.add(networkFile);
	}

	@Override
	public String toString() {
		return "NetworkFile [path=" + path + ", name=" + name + ", format=" + format + ", directory=" + directory + "]";
	}

	public boolean isImage() {
		return image;
	}

	public void setImage(boolean image) {
		this.image = image;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}