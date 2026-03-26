package com.zoho.notebook.controller;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.service.NotebookService;
import com.zoho.notebook.util.ResponseMapper;
import com.zoho.notebook.util.RequestValidator;
import com.zoho.notebook.util.FrequentlyUsedMethods;
import com.zoho.notebook.dto.request.NotebookDtoRequest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NotebookServlet extends BaseServlet {
    
    private static final long serialVersionUID = 1L;
    private final NotebookService notebookService = new NotebookService();
    private static final Logger LOGGER = Logger.getLogger(NotebookServlet.class.getName());

    private boolean isNoteRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (uri.contains("/notes")) {
            try {
                // Extract Notebook ID from /notebooks/{id}/notes...
                String[] parts = uri.split("/");
                if (parts.length > 2) {
                    long notebookId = Long.parseLong(parts[2]);
                    
                    // Validate Notebook existence
                    NotebookModel notebook = notebookService.getNotebookById(notebookId);
                    if (notebook == null) {
                        LOGGER.warning("Notebook not found during handoff: " + notebookId);
                        sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
                        return true;
                    }

                    // Set attribute for NoteServlet and forward
                    req.setAttribute("notebookId", notebookId);
                    LOGGER.info("Forwarding to NoteServlet for notebookId: " + notebookId);
                    req.getServletContext().getNamedDispatcher("NoteServlet").forward(req, resp);
                    return true;
                }
            } catch (Exception e) {
                LOGGER.severe("Error during NoteServlet forwarding: " + e.getMessage());
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid Notebook ID");
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isNoteRequest(req, resp)) return;

        try {
            Long notebookId = FrequentlyUsedMethods.getPathId(req);
            if (notebookId != null) {
                NotebookModel notebook = notebookService.getNotebookById(notebookId);
                if (notebook != null) {
                    sendSuccess(resp, "Notebook Fetched Successfully", ResponseMapper.toNotebookResponse(notebook));
                } else {
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
                }
            } else {
                List<NotebookModel> notebooks = notebookService.getUserNotebooks();
                sendSuccess(resp, "Notebooks Fetched Successfully", ResponseMapper.toNotebookListResponse(notebooks));
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isNoteRequest(req, resp)) return;
        try {
            NotebookDtoRequest request = FrequentlyUsedMethods.readJson(req, NotebookDtoRequest.class);
            if (!RequestValidator.validateNotebook(request)) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title is required");
                return;
            }
            if (notebookService.createNotebook(request)) {
                sendSuccess(resp, "Notebook created successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Creation failed");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isNoteRequest(req, resp)) return;
        try {
            Long notebookId = FrequentlyUsedMethods.getPathId(req);
            if (notebookId == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID required");
                return;
            }
            NotebookDtoRequest request = FrequentlyUsedMethods.readJson(req, NotebookDtoRequest.class);
            if (notebookService.updateNotebook(notebookId, request)) {
                sendSuccess(resp, "Notebook updated successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (isNoteRequest(req, resp)) return;
        try {
            Long notebookId = FrequentlyUsedMethods.getPathId(req);
            if (notebookId == null) {
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "ID required");
                return;
            }
            if (notebookService.deleteNotebook(notebookId)) {
                sendSuccess(resp, "Notebook deleted successfully");
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
            }
        } catch (Exception e) {
            sendServerError(resp, e);
        }
    }
}