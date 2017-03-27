package com.elipcero.springintegrationdemo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

// It preprocess xml configuration replacing bean ids, before of generating GenericXmlApplicationContext.
// The bean ids can't be populated using environment variables
public class PreProcessorXmlApplicationContext extends GenericXmlApplicationContext {
	
	public PreProcessorXmlApplicationContext(ClassPathResource pathResource, ApplicationContext parent, Map<String, String> preProcessorProps) throws IOException {
		setParent(parent);
		load(preProcessorXmlConfiguration(pathResource, preProcessorProps));
	}

	private ByteArrayResource preProcessorXmlConfiguration(ClassPathResource pathResource, Map<String, String> preProcessorProps) throws IOException {

		String xml;

		InputStream stream = pathResource.getInputStream();
		try {
			xml = StreamUtils.copyToString(stream, Charset.forName("UTF-8"));
		}
		finally {
			stream.close();	
		}
		
		for (Map.Entry<String, String> preProcessorProp : preProcessorProps.entrySet()) {
		    xml = StringUtils.replace(xml, preProcessorProp.getKey(), preProcessorProp.getValue());
		}
		
		return new ByteArrayResource(xml.getBytes("UTF-8"));
	}
}
	