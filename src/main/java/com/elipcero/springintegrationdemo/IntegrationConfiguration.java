package com.elipcero.springintegrationdemo;

import java.io.File;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.http.Http;
import org.springframework.integration.dsl.support.Transformers;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class IntegrationConfiguration {
	
	@Autowired
	private DynamicFtpWatcher dynamicFtpWatcher;

	@Autowired
	private FtpTransformer ftpTransformer;
	
	@Bean
	public AcceptAllFileListFilter<File> acceptAllFileListFilter() {
		return new AcceptAllFileListFilter<File>();
	}
	
	@Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
         
        dataSource.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerConnection");
        dataSource.setUrl("jdbc:sqlserver://192.168.100.153:1433;databaseName=integrationdb;user=sa;password=!qa2ws3ed4rf;");
        dataSource.setUsername("sa");
        dataSource.setPassword("!qa2ws3ed4rf");
         
        return dataSource;
    }
	
	@Bean
	public MessageSource<Object> jdbcMessageSource() {
		return new JdbcInMemoryChangeWatcherAdapter(dataSource(),"ftpconfiguration", "timestamp", "id");
	}	
	
	@Bean
	public IntegrationFlow JdbcWatcherFlow() {
		return IntegrationFlows.from(this.jdbcMessageSource(), 
							c -> c.poller(Pollers.fixedRate(5000).maxMessagesPerPoll(1)))
				.handle(m -> dynamicFtpWatcher.watcher(m.getPayload()))
		        .get();
	}
	
	@Bean
	public DirectChannel ftpToChannel() {
	    return MessageChannels.direct().get();
	}	
	
	@Bean
	public IntegrationFlow ftpToChannelFlow() {
	     return IntegrationFlows
    		 .from(ftpToChannel())
    		 .transform(Transformers.fileToString())
    		 .transform(ftpTransformer, "transformFileResponse")
    		 .transform(Transformers.toJson())
    		 .handle(Http.outboundGateway("http://localhost:8080/receiver")
    				 	.httpMethod(HttpMethod.POST)
    				 	.charset("utf-8")
    				 	.extractPayload(true)
    				 	.expectedResponseType(String.class)
    				 )
    		 .channel(ftpOutboundChannel())
             .get();
	}
	
	@Bean
	public DirectChannel ftpOutboundChannel() {
	    return MessageChannels.direct().get();
	}
	
	@Bean
	public DirectChannel endChannel() {
	    return MessageChannels.direct().get();
	}
	
	@Bean
	public IntegrationFlow endChannelFlow() {
	     return IntegrationFlows
    		 .from(endChannel())
    		 .handle(System.out::println)
             .get();
	}
}
