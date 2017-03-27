package com.elipcero.springintegrationdemo;

import java.util.Map;

public class ChangedRowInformation {
	
	public enum EnumState {
		init,
		added,
		modified,
		deleted
	}
	
	private final EnumState state;
	
	private String primaryKeyColumnName;
	
	public String getPrimaryKeyColumnName() {
		return this.primaryKeyColumnName;
	}

	public String getPrimaryKeyValue() {
		return getRow().get(this.primaryKeyColumnName).toString();
	}
	
	public EnumState getState() {
		return state;
	}
	
	public Object getValue(String columnName) {
		return getRow().get(columnName);
	}

	public Map<String, Object> getRow() {
		return row;
	}

	private final Map<String, Object> row;
	
	public ChangedRowInformation(EnumState state, String primaryKeyColumnName, Map<String, Object> row) {
		this.primaryKeyColumnName = primaryKeyColumnName;
		this.state = state;
		this.row = row;
	}
}
