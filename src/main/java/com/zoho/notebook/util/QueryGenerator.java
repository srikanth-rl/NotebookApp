package com.zoho.notebook.util;

import java.util.ArrayList;
import java.util.List;

public class QueryGenerator {

	private final StringBuilder query = new StringBuilder();
	private final List<Object> params = new ArrayList<>();
	private List<Condition> conditions;
	
	public void select(String columns) {
		query.append("SELECT ").append(columns).append(" ");
	}

	public void from(String table) {
		query.append("FROM ").append(table).append(" ");
	}

	public void where(List<Condition> conditions, String operator) {
		this.conditions = conditions;
		if (conditions == null || conditions.isEmpty())
			return;

		query.append("WHERE ");

		if (operator == null || operator.isEmpty())
			operator = "AND";

		for (int i = 0; i < conditions.size(); i++) {
			Condition c = conditions.get(i);
			query.append(c.getColumn()).append(" ").append(c.getOperator()).append(" ?");

			params.add(c.getValue());

			if (i < conditions.size() - 1) {
				query.append(" ").append(operator).append(" ");
			}
		}
		query.append(" ");
	}
	
	public Long getId() {
        if (conditions == null) 
        	return null;
        for (Condition c : conditions) {
            if ("id".equalsIgnoreCase(c.getColumn())) {
                Object val = c.getValue();
                if (val instanceof Long) return (Long) val;
                if (val instanceof Integer) return ((Integer) val).longValue();
                if (val instanceof String) return Long.parseLong((String) val);
            }
        }
        return null;
    }
	
	public void orderBy(List<String> sortColumns) {
		if (sortColumns == null || sortColumns.isEmpty())
			return;

		query.append("ORDER BY ");
		for (int i = 0; i < sortColumns.size(); i++) {
			query.append(sortColumns.get(i));
			if (i < sortColumns.size() - 1) {
				query.append(", ");
			}
		}
		query.append(" ");
	}

	public void join(String table, String onCondition) {
		join("JOIN", table, onCondition);
	}

	public void join(String joinType, String table, String onCondition) {
		if (table == null || onCondition == null)
			return;

		query.append(joinType).append(" ").append(table).append(" ON ").append(onCondition).append(" ");
	}

	public void insertInto(String table, String columns) {
		query.append("INSERT INTO ").append(table).append(" (").append(columns).append(") ");
	}

	public void values(int count) {
		query.append("VALUES (");
		for (int i = 0; i < count; i++) {
			if (i > 0)
				query.append(",");
			query.append("?");
		}
		query.append(") ");
	}

	public void addParam(Object value) {
		params.add(value);
	}

	public void update(String table) {
		query.append("UPDATE ").append(table).append(" ");
	}

	public void set(List<String> columns, List<Object> values) {
		query.append("SET ");
		for (int i = 0; i < columns.size(); i++) {
			query.append(columns.get(i)).append(" = ?");
			params.add(values.get(i));

			if (i < columns.size() - 1)
				query.append(", ");
		}
		query.append(" ");
	}
	
	public void delete(String table) {
        query.append("DELETE FROM ").append(table).append(" ");
    }
	
	public List<Object> getParams() {
		return params;
	}
	
	public String build() {
		return query.toString();
	}
}