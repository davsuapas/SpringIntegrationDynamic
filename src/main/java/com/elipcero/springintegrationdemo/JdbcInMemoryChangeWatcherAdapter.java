package com.elipcero.springintegrationdemo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.messaging.Message;

// It detect changes of a table
// The updating is detected using a stamp field (watchField)
// Only accept primary key of string type
public class JdbcInMemoryChangeWatcherAdapter extends IntegrationObjectSupport implements MessageSource<Object> {

	private final NamedParameterJdbcOperations jdbcOperations;

	private final String tableName;
	
	private final String watchField;
	private final String primaryKey;
	
	private List<Map<String, Object>> memoryTable; 

	public JdbcInMemoryChangeWatcherAdapter(DataSource dataSource, String tableName, String watchField, String primaryKey) {
		this.jdbcOperations = new NamedParameterJdbcTemplate(dataSource);
		this.tableName = tableName;
		this.watchField = watchField;
		this.primaryKey = primaryKey;
		memoryTable = new ArrayList<Map<String, Object>>();
	}

	public JdbcInMemoryChangeWatcherAdapter(JdbcOperations jdbcOperations, String tableName, String watchField, String primaryKey) {
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations);
		this.tableName = tableName;
		this.watchField = watchField;
		this.primaryKey = primaryKey;
		memoryTable = new ArrayList<Map<String, Object>>();
	}
	
	@Override
	public Message<Object> receive() {
		
		List<ChangedRowInformation> payload = doPoll();
		
		if (payload.size() == 0) {
			return null;
		}
		
		return this.getMessageBuilderFactory().withPayload((Object)payload).build();
	}

	// Compare all physic table information with the memory table.
	// After comparing it saves in memory the physic table
	// This adapter is only prepared for small tables (normally configuration tables)
	// Return all changes detected (added, deleted, updated)
	private List<ChangedRowInformation> doPoll() {
		
		String selectQuery = String.format("select * from %s order by %s", tableName, primaryKey); 
		
		ResultSetExtractor<List<Map<String, Object>>> mapper =
				new RowMapperResultSetExtractor<Map<String, Object>>(new ColumnMapRowMapper());
				
		List<Map<String, Object>> result = this.jdbcOperations.query(selectQuery, mapper);
		
		List<ChangedRowInformation> changedRows = new ArrayList<ChangedRowInformation>();
		
		if (memoryTable.size() == 0) {
			result.stream().forEach(
					r -> changedRows.add(new ChangedRowInformation(ChangedRowInformation.EnumState.init, primaryKey, r)));
		}
		else {
			Iterator<Map<String, Object>> iteratorSelect = result.iterator();
			Iterator<Map<String, Object>> iteratorMemoryTable = memoryTable.iterator();
				
			Map<String, Object> rowSelect = null;
			Map<String, Object> rowMemoryTable = null;
	
			rowSelect = interatorNextOrNull(iteratorSelect);
			rowMemoryTable = interatorNextOrNull(iteratorMemoryTable);
			
			while (rowSelect != null || rowMemoryTable != null ) {
				
				if (rowMemoryTable == null || 
						rowSelect != null && rowSelect.get(primaryKey).toString().compareTo((String)rowMemoryTable.get(primaryKey)) < 0) {
					
					changedRows.add(new ChangedRowInformation(ChangedRowInformation.EnumState.added, primaryKey, rowSelect));
					
					rowSelect = interatorNextOrNull(iteratorSelect);
					
				} else if (rowSelect == null || 
						rowMemoryTable != null && rowSelect.get(primaryKey).toString().compareTo((String)rowMemoryTable.get(primaryKey)) > 0) {
					
					changedRows.add(new ChangedRowInformation(ChangedRowInformation.EnumState.deleted, primaryKey, rowMemoryTable));
					
					rowMemoryTable = interatorNextOrNull(iteratorMemoryTable);
					
				} else {
					
					if (rowSelect.get(watchField).toString().compareTo((String)rowMemoryTable.get(watchField)) != 0) {
						changedRows.add(new ChangedRowInformation(ChangedRowInformation.EnumState.modified, primaryKey, rowSelect));	
					}
					
					rowSelect = interatorNextOrNull(iteratorSelect);
					rowMemoryTable = interatorNextOrNull(iteratorMemoryTable);
				}
			}
		}
		
		memoryTable = result;
		
		return changedRows;
	}
	
	private static Map<String, Object> interatorNextOrNull(Iterator<Map<String, Object>> iterator)  {
		
		if (iterator.hasNext()) {
			return iterator.next();
		}
		else {
			return null;
		}
	}
	 
	@Override
	public String getComponentType() {
		return "jdbc:inmemorychangewatcher-channel-adapter";
	}
}

