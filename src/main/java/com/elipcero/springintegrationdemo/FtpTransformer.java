package com.elipcero.springintegrationdemo;

import org.springframework.integration.annotation.Transformer;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class FtpTransformer {
	
	@Transformer
	public FileResponse transformFileResponse(String content, @Header("file_name")String fileName) {
		return new FileResponse(fileName, content);
	}	
}
