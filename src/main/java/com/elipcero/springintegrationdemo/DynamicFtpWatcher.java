package com.elipcero.springintegrationdemo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DynamicFtpWatcher {
	
	@Autowired
	private ApplicationContext appContext;
	
	private Map<String, ConfigurableApplicationContext> ftpContext = new HashMap<String, ConfigurableApplicationContext>();
	
	@SuppressWarnings("unchecked")
	public synchronized void watcher(Object playLoad) {
		
		for (ChangedRowInformation changedRowInformation : (List<ChangedRowInformation>)playLoad) {
			
			switch (changedRowInformation.getState()) {
				case added: case init:
					add(changedRowInformation);
					break;
				case modified:
					update(changedRowInformation);
					break;
				case deleted:
					remove(changedRowInformation);
					break;
				default:
					break;
			}
		}
	}
	
	@SuppressWarnings("serial")
	private void add(ChangedRowInformation changedRowInformation) {
	
		try {
			ConfigurableApplicationContext ftpApplicationContext = new PreProcessorXmlApplicationContext(
					new ClassPathResource("/FtpDynamic.xml"),
					appContext,
					new HashMap<String, String>() {{
					    put("@Id", changedRowInformation.getPrimaryKeyValue().trim());
					}} 
				);
			
			ftpApplicationContext.setEnvironment(getEnvironment(changedRowInformation));
			ftpApplicationContext.refresh();
			
			ftpContext.put(changedRowInformation.getPrimaryKeyValue(), ftpApplicationContext);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void remove(ChangedRowInformation changedRowInformation) {
		ftpContext.get(changedRowInformation.getPrimaryKeyValue()).close();
		ftpContext.remove(changedRowInformation.getPrimaryKeyValue());
	}
	
	private void update(ChangedRowInformation changedRowInformation) {
		remove(changedRowInformation);
		add(changedRowInformation);
	}
	
	private static StandardEnvironment getEnvironment(ChangedRowInformation changedRowInformation) {
		StandardEnvironment ftpEnvironment = new StandardEnvironment();
		
		Properties props = new Properties();
		props.setProperty("id", changedRowInformation.getPrimaryKeyValue().trim());
		props.setProperty("host", changedRowInformation.getValue("host").toString().trim());
		props.setProperty("userName", changedRowInformation.getValue("username").toString().trim());
		props.setProperty("password", changedRowInformation.getValue("password").toString().trim());
		props.setProperty("clientMode", changedRowInformation.getValue("clientmode").toString().trim());
		props.setProperty("pattern", changedRowInformation.getValue("pattern").toString().trim());
		
		PropertiesPropertySource pps = new PropertiesPropertySource("dynamicftpproperties", props);
		ftpEnvironment.getPropertySources().addLast(pps);
		
		return ftpEnvironment;
	}	
	
	@PreDestroy
	public void destroy(){
		for (ConfigurableApplicationContext context : ftpContext.values()) {
			context.close();
		}
	}	
}
