package com.zoho.notebook.dao;

import com.adventnet.ds.query.Column;
import com.adventnet.ds.query.Criteria;
import com.adventnet.ds.query.QueryConstants;
import com.adventnet.ds.query.SelectQueryImpl;
import com.adventnet.ds.query.SortColumn;
import com.adventnet.ds.query.Table;
import com.adventnet.ds.query.UpdateQueryImpl;
import com.adventnet.ds.query.DataSet;
import com.adventnet.db.api.RelationalAPI;
import com.adventnet.persistence.DataAccess;
import com.adventnet.persistence.DataObject;
import com.adventnet.persistence.Row;
import com.adventnet.persistence.WritableDataObject;

import com.zoho.notebook.enums.OwnerType;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class NotebookDao {

    public Long insert(NotebookModel notebook) throws Exception {
        long now = System.currentTimeMillis();

        Row r = new Row(TableName.Notebook.name());
        r.set("TITLE",      notebook.getTitle());
        r.set("OWNER_TYPE", notebook.getOwnerType());
        r.set("OWNER_ID",   notebook.getOwnerId());
        r.set("CREATED_BY", notebook.getCreatedBy());
        r.set("UPDATED_BY", notebook.getUpdatedBy());
        r.set("CREATED_AT", now);
        r.set("UPDATED_AT", now);
        

        DataObject d = new WritableDataObject();
        d.addRow(r);
        DataAccess.add(d);

        Row inserted = d.getRow(TableName.Notebook.name());
        if (inserted == null) {
            return null;
        }
        Object id = inserted.get("ID");
        return id != null ? ((Number) id).longValue() : null;
    }

    public List<NotebookModel> getAllNotebooksByOwner(long ownerId, OwnerType ownerType, String userTimezone) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.Notebook.name()));
        q.addSelectColumn(new Column(TableName.Notebook.name(), "*"));
        Criteria c = new Criteria(new Column(TableName.Notebook.name(), "OWNER_ID"),  ownerId,          QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column(TableName.Notebook.name(), "OWNER_TYPE"),   ownerType.name(), QueryConstants.EQUAL));
        q.setCriteria(c);
        q.addSortColumn(new SortColumn(new Column(TableName.Notebook.name(), "UPDATED_AT"), false));

        List<NotebookModel> list = new ArrayList<>();
        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            while (ds.next()) {
                list.add(setAndGetNotebookFullDetails(ds, userTimezone));
            }
        } finally {
            conn.close();
        }
        return list;
    }

    public NotebookModel getNotebookById(long ownerId, long id) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.Notebook.name()));
        q.addSelectColumn(new Column(TableName.Notebook.name(), "ID"));
        q.addSelectColumn(new Column(TableName.Notebook.name(), "TITLE"));
        q.addSelectColumn(new Column(TableName.Notebook.name(), "OWNER_ID"));
        Criteria c = new Criteria(new Column(TableName.Notebook.name(), "ID"),       id,      QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column(TableName.Notebook.name(), "OWNER_ID"),    ownerId, QueryConstants.EQUAL));
        q.setCriteria(c);

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (!ds.next()) {
                return null;
            }
            NotebookModel nb = new NotebookModel();
            nb.setId(((Number) ds.getValue("ID")).longValue());
            nb.setTitle((String) ds.getValue("TITLE"));
            nb.setOwnerId(((Number) ds.getValue("OWNER_ID")).longValue());
            return nb;
        } finally {
            conn.close();
        }
    }

    public boolean update(long id, String title, long userId) throws Exception {
        UpdateQueryImpl q = new UpdateQueryImpl(TableName.Notebook.name());
        q.setUpdateColumn("TITLE",      title);
        q.setUpdateColumn("UPDATED_BY", userId);
        q.setUpdateColumn("UPDATED_AT", System.currentTimeMillis());
        q.setCriteria(new Criteria(new Column(TableName.Notebook.name(), "ID"), id, QueryConstants.EQUAL));
        DataAccess.update(q);
        return true;
    }

    public boolean delete(long id) throws Exception {
        Criteria c = new Criteria(new Column(TableName.Notebook.name(), "ID"), id, QueryConstants.EQUAL);
        DataAccess.delete(c);
        return true;
    }

    private NotebookModel setAndGetNotebookFullDetails(DataSet ds, String userTimezone) throws Exception {
        NotebookModel nb = new NotebookModel();
        nb.setId(((Number) ds.getValue("ID")).longValue());
        nb.setTitle((String) ds.getValue("TITLE"));
        nb.setOwnerType((String) ds.getValue("OWNER_TYPE"));
        nb.setOwnerId(((Number) ds.getValue("OWNER_ID")).longValue());
        nb.setCreatedBy(((Number) ds.getValue("CREATED_BY")).longValue());
        nb.setUpdatedBy(((Number) ds.getValue("UPDATED_BY")).longValue());
        nb.setCreatedAt(FrequentlyUsedMethods.formatMsToTimezone(((Number) ds.getValue("CREATED_AT")).longValue(), userTimezone));
        nb.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimezone(((Number) ds.getValue("UPDATED_AT")).longValue(), userTimezone));
        return nb;
    }
}
