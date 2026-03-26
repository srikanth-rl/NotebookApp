package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.zoho.notebook.model.NoteModel;
import com.zoho.notebook.service.NoteService;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.dto.request.NoteDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NoteServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;
    private final NoteService noteService = new NoteService();
    private static final Logger LOGGER = Logger.getLogger(NoteServlet.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long notebookId = (long) req.getAttribute("notebookId");
            Long noteId = getNoteIdFromURI(req);

            if (noteId != null) {
                LOGGER.info("Fetching note detail: noteId=" + noteId);
                NoteModel note = noteService.getNoteById(notebookId, noteId);
                if (note != null) {
                    sendSuccess(resp, "Note Fetched Successfully", ResponseMapper.toNoteResponse(note));
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Note not found");
                }
            } else {
                LOGGER.info("Fetching all notes for notebookId=" + notebookId);
                List<NoteModel> notes = noteService.getNotesByNotebookId(notebookId);
                sendSuccess(resp, "Notes Fetched Successfully", ResponseMapper.toNoteListResponse(notes));
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long notebookId = (long) req.getAttribute("notebookId");
            NoteDtoRequest request = FrequentlyUsedMethods.readJson(req, NoteDtoRequest.class);
            if (!RequestValidator.validateNote(request)) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title is required");
                return;
            }
            if (noteService.createNote(notebookId, request)) {
                sendSuccess(resp, "Note created successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Creation failed");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long notebookId = (long) req.getAttribute("notebookId");
            Long noteId = getNoteIdFromURI(req);
            if (noteId == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Note ID required");
                return;
            }
            NoteDtoRequest request = FrequentlyUsedMethods.readJson(req, NoteDtoRequest.class);
            if (noteService.updateNote(notebookId, noteId, request)) {
                sendSuccess(resp, "Note updated successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Note not found");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            long notebookId = (long) req.getAttribute("notebookId");
            Long noteId = getNoteIdFromURI(req);
            if (noteId == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Note ID required");
                return;
            }
            if (noteService.deleteNote(notebookId, noteId)) {
                sendSuccess(resp, "Note deleted successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Note not found");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    private Long getNoteIdFromURI(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String[] parts = uri.split("/");
        if (parts.length > 4 && parts[4] != null && !parts[4].isEmpty()) {
            return Long.parseLong(parts[4]);
        }
        return null;
    }
}