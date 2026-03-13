package com.zoho.notebook.dao;


import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.enums.OwnerType;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.util.Condition;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.util.QueryExecutor;
import com.zoho.notebook.util.QueryGenerator;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.List;
import java.util.ArrayList;

public class NotebookDao {

	public Long insert(Connection con, NotebookModel notebook) throws Exception {
		QueryGenerator q = new QueryGenerator();
		long now = System.currentTimeMillis();
		
		q.insertInto(TableName.notebooks.name(),
				"title, owner_type, owner_id, created_by, updated_by, created_at, updated_at");
		q.values(7);
		q.addParam(notebook.getTitle());
		q.addParam(notebook.getOwnerType());
		q.addParam(notebook.getOwnerId());
		q.addParam(notebook.getCreatedBy());
		q.addParam(notebook.getUpdatedBy());
		q.addParam(now);
		q.addParam(now);

		return QueryExecutor.executeInsert(con, q);
	}

	public List<NotebookModel> getAllNotebooksByOwner(Connection con, long ownerId, OwnerType ownerType, String userTimeZone)
			throws Exception {
		QueryGenerator q = new QueryGenerator();

		q.select("*");
		q.from(TableName.notebooks.name());
		q.where(List.of(
				new Condition("owner_id", "=", ownerId), 
				new Condition("owner_type", "=", ownerType.name())),
				"AND");
		q.orderBy(List.of("updated_at DESC"));

		List<NotebookModel> list = new ArrayList<>();
		try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
			while (rs.next()) {
				list.add(setAndGetNotebookFullDetails(rs, userTimeZone));
			}
		}

		return list;
	}

	public NotebookModel getNotebookById(Connection con, long ownerId, long id) throws Exception {
		QueryGenerator q = new QueryGenerator();
		NotebookModel nb = new NotebookModel();

		q.select("id, title, owner_id");
		q.from(TableName.notebooks.name());
		q.where(List.of(new Condition("id", "=", id), new Condition("owner_id", "=", ownerId)), "AND");

		try (ResultSet rs = QueryExecutor.executeQuery(con, q)) {
			if (!rs.next()) {
				return null;
			}

			nb.setId(rs.getLong("id"));
			nb.setTitle(rs.getString("title"));
			nb.setOwnerId(rs.getLong("owner_id"));
		}

		return nb;
	}

	public boolean update(Connection con, long id, String title, long userId) throws Exception {
		QueryGenerator q = new QueryGenerator();

		q.update(TableName.notebooks.name());
		q.set(List.of("title", "updated_by", "updated_at"),
				List.of(title, userId, System.currentTimeMillis()));
		q.where(List.of(new Condition("id", "=", id)), "AND");

		int rows = QueryExecutor.executeUpdate(con, q);
		return rows > 0;
	}

	public boolean delete(Connection con, long id) throws Exception {
		QueryGenerator q = new QueryGenerator();

		q.delete(TableName.notebooks.name());
		q.where(List.of(new Condition("id", "=", id)), "AND");

		int rows = QueryExecutor.executeUpdate(con, q);
		return rows > 0;
	}

	private NotebookModel setAndGetNotebookFullDetails(ResultSet rs, String userTimeZone) throws Exception {
		NotebookModel nb = new NotebookModel();

		nb.setId(rs.getLong("id"));
		nb.setTitle(rs.getString("title"));
		nb.setOwnerType(rs.getString("owner_type"));
		nb.setOwnerId(rs.getLong("owner_id"));
		nb.setCreatedBy(rs.getLong("created_by"));
		nb.setUpdatedBy(rs.getLong("updated_by"));
		nb.setCreatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("created_at"), userTimeZone));
        nb.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimeZone(rs.getLong("updated_at"), userTimeZone));

		return nb;
	}
}