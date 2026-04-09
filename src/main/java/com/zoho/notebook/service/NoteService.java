package com.zoho.notebook.service;

import com.adventnet.persistence.DataAccess;
import javax.transaction.TransactionManager;

import com.zoho.notebook.dao.NoteDao;
import com.zoho.notebook.model.NoteModel;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.dto.request.NoteDtoRequest;

import java.util.List;
import java.util.logging.Logger;

public class NoteService {

    private final NoteDao noteDao = new NoteDao();
    private static final Logger LOGGER = Logger.getLogger(NoteService.class.getName());

    public boolean createNote(long notebookId, NoteDtoRequest req) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean created = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();

            NoteModel note = new NoteModel();
            note.setNotebookId(notebookId);
            note.setTitle(req.getTitle());
            note.setContent(req.getContent());
            note.setPinned(req.getIsPinned() != null && req.getIsPinned());
            note.setCreatedBy(userId);
            note.setUpdatedBy(userId);

            Long generatedNoteId = noteDao.insert(note);

            if (generatedNoteId != null && generatedNoteId > 0) {
                txMgr.commit();
                created = true;
                LOGGER.info("Note created successfully. noteId=" + generatedNoteId + ", notebookId=" + notebookId + ", userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Note creation failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return created;
    }

    public List<NoteModel> getNotesByNotebookId(long notebookId) throws Exception {
        return noteDao.getNotesByNotebookId(notebookId, UserSessionContext.getTimezone());
    }

    public NoteModel getNoteById(long notebookId, long noteId) throws Exception {
        return noteDao.getNoteById(noteId, notebookId);
    }

    public boolean updateNote(long notebookId, long noteId, NoteDtoRequest req) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean updated = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();
            NoteModel note = noteDao.getNoteById(noteId, notebookId);

            if (note == null) {
                LOGGER.warning("Note not found for update. noteId=" + noteId + ", notebookId=" + notebookId);
                txMgr.rollback();
                return false;
            }

            updated = noteDao.update(noteId, req, userId);

            if (updated) {
                txMgr.commit();
                LOGGER.info("Note updated successfully. noteId=" + noteId + ", userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Note update failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return updated;
    }

    public boolean deleteNote(long notebookId, long noteId) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean deleted = false;

        try {
            txMgr.begin();

            NoteModel note = noteDao.getNoteById(noteId, notebookId);

            if (note == null) {
                LOGGER.warning("Note not found for deletion. noteId=" + noteId + ", notebookId=" + notebookId);
                txMgr.rollback();
                return false;
            }

            deleted = noteDao.delete(noteId);

            if (deleted) {
                txMgr.commit();
                LOGGER.info("Note deleted successfully. noteId=" + noteId + ", notebookId=" + notebookId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Note deletion failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return deleted;
    }
}