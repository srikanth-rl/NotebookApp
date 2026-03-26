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

import com.zoho.notebook.dto.request.NoteDtoRequest;
import com.zoho.notebook.enums.TableName;
import com.zoho.notebook.model.NoteModel;
import com.zoho.notebook.util.FrequentlyUsedMethods;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class NoteDao {

    public Long insert(NoteModel note) throws Exception {
        long now = System.currentTimeMillis();

        Row r = new Row(TableName.Note.name());
        r.set("NOTEBOOK_ID", note.getNotebookId());
        r.set("TITLE",       note.getTitle());
        r.set("CONTENT",     note.getContent());
        r.set("IS_PINNED",   note.isPinned());
        r.set("CREATED_BY",  note.getCreatedBy());
        r.set("UPDATED_BY",  note.getUpdatedBy());
        r.set("CREATED_AT",  now);
        r.set("UPDATED_AT",  now);

        DataObject d = new WritableDataObject();
        d.addRow(r);
        DataAccess.add(d);

        Row inserted = d.getRow(TableName.Note.name());
        if (inserted == null)
            return null;

        Object id = inserted.get("ID");
        return id != null ? ((Number) id).longValue() : null;
    }

    public List<NoteModel> getNotesByNotebookId(long notebookId, String userTimeZone) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.Note.name()));
        q.addSelectColumn(new Column(TableName.Note.name(), "*"));
        q.setCriteria(new Criteria(new Column(TableName.Note.name(), "NOTEBOOK_ID"), notebookId, QueryConstants.EQUAL));
        // Pinned notes first, then most recently updated
        q.addSortColumn(new SortColumn(new Column(TableName.Note.name(), "IS_PINNED"), false));
        q.addSortColumn(new SortColumn(new Column(TableName.Note.name(), "UPDATED_AT"), false));

        List<NoteModel> list = new ArrayList<>();
        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            while (ds.next()) {
                list.add(mapToNoteModel(ds, userTimeZone));
            }
        } finally {
            conn.close();
        }
        return list;
    }

    public NoteModel getNoteById(long noteId, long notebookId) throws Exception {
        SelectQueryImpl q = new SelectQueryImpl(new Table(TableName.Note.name()));
        q.addSelectColumn(new Column(TableName.Note.name(), "*"));
        Criteria c = new Criteria(new Column(TableName.Note.name(), "ID"),          noteId,     QueryConstants.EQUAL);
        c = c.and(new Criteria(new Column(TableName.Note.name(), "NOTEBOOK_ID"),    notebookId, QueryConstants.EQUAL));
        q.setCriteria(c);

        RelationalAPI api = RelationalAPI.getInstance();
        Connection conn = api.getConnection();
        try (DataSet ds = api.executeQuery(q, conn)) {
            if (!ds.next())
                return null;
            return mapToNoteModel(ds, null);
        } finally {
            conn.close();
        }
    }

    public boolean update(long noteId, NoteDtoRequest req, long userId) throws Exception {
        long now = System.currentTimeMillis();

        UpdateQueryImpl q = new UpdateQueryImpl(TableName.Note.name());
        if (FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getTitle()))
            q.setUpdateColumn("TITLE",     req.getTitle());
        if (FrequentlyUsedMethods.isNotNullAndNotEmpty(req.getContent()))
            q.setUpdateColumn("CONTENT",   req.getContent());
        if (req.getIsPinned() != null)
            q.setUpdateColumn("IS_PINNED", req.getIsPinned());
        q.setUpdateColumn("UPDATED_BY", userId);
        q.setUpdateColumn("UPDATED_AT", now);
        q.setCriteria(new Criteria(new Column(TableName.Note.name(), "ID"), noteId, QueryConstants.EQUAL));
        DataAccess.update(q);
        return true;
    }

    public boolean delete(long noteId) throws Exception {
        Criteria c = new Criteria(new Column(TableName.Note.name(), "ID"), noteId, QueryConstants.EQUAL);
        DataAccess.delete(c);
        return true;
    }

    private NoteModel mapToNoteModel(DataSet ds, String userTimeZone) throws Exception {
        NoteModel note = new NoteModel();
        note.setId(((Number) ds.getValue("ID")).longValue());
        note.setNotebookId(((Number) ds.getValue("NOTEBOOK_ID")).longValue());
        note.setTitle((String) ds.getValue("TITLE"));
        note.setContent((String) ds.getValue("CONTENT"));
        note.setPinned((Boolean) ds.getValue("IS_PINNED")); // BOOLEAN in data-dict → Boolean from JDBC
        note.setCreatedBy(((Number) ds.getValue("CREATED_BY")).longValue());
        note.setUpdatedBy(((Number) ds.getValue("UPDATED_BY")).longValue());
        if (userTimeZone != null) {
            note.setCreatedAt(FrequentlyUsedMethods.formatMsToTimeZone(((Number) ds.getValue("CREATED_AT")).longValue(), userTimeZone));
            note.setUpdatedAt(FrequentlyUsedMethods.formatMsToTimeZone(((Number) ds.getValue("UPDATED_AT")).longValue(), userTimeZone));
        }
        return note;
    }
}