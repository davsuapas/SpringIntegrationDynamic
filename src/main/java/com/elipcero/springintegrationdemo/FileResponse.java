package com.elipcero.springintegrationdemo;

public class FileResponse {

	private String name;
	
	private String content;
	
	public FileResponse() {
	}

	public FileResponse(String name, String content) {
		this.name = name;
		this.content = content;
	}

	public String getName() {
		return name;
	}

	public String getContent() {
		return content;
	}
	
	
}
