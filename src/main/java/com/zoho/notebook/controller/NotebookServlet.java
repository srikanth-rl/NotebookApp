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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Notebook fetch request received");

            Long notebookId = FrequentlyUsedMethods.getPathId(req);

            if (notebookId != null) {
                LOGGER.info("Fetching notebook by id");

                NotebookModel notebook = notebookService.getNotebookById(notebookId);
                
                if (notebook != null) {
                    LOGGER.info("Notebook fetched successfully");
                    sendSuccess(resp, "Notebook Fetched Successfully", ResponseMapper.toNotebookResponse(notebook));
                } else {
                    LOGGER.warning("Notebook not found");
                    sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
                }
                
            } else {
                LOGGER.info("Fetching all notebooks for user");

                List<NotebookModel> notebooks = notebookService.getUserNotebooks();
                sendSuccess(resp, "Notebooks Fetched Successfully", ResponseMapper.toNotebookListResponse(notebooks));
            }
        } catch (Exception e) {
            LOGGER.severe("Error while fetching notebook: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Create notebook request received");

            NotebookDtoRequest request = FrequentlyUsedMethods.readJson(req, NotebookDtoRequest.class);
            
            if (!RequestValidator.validateNotebook(request)) {
                LOGGER.warning("Invalid notebook data provided");
                sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title is required");
                return;
            }
            
            boolean created = notebookService.createNotebook(request);
            
            if (created) {
                LOGGER.info("Notebook created successfully");
                sendSuccess(resp, "Notebook created successfully");
            } else {
                LOGGER.warning("Notebook creation failed");
                sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Creation failed");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while creating notebook: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Update notebook request received");

            Long notebookId = FrequentlyUsedMethods.getPathId(req);
            
            if (notebookId == null) {
                LOGGER.warning("Invalid notebook id provided in path");
            	sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path: ID must be a number");
            }

            NotebookDtoRequest request = FrequentlyUsedMethods.readJson(req, NotebookDtoRequest.class);
            
            if (!RequestValidator.validateNotebook(request)) {
                LOGGER.warning("Invalid notebook update data provided");
            	sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Title is required");
                return;
            }

            boolean updated = notebookService.updateNotebook(notebookId, request);
            
            if (updated) {
                LOGGER.info("Notebook updated successfully");
                sendSuccess(resp, "Notebook updated successfully");
            } else {
                LOGGER.warning("Notebook not found for update");
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while updating notebook: " + e.getMessage());
            sendServerError(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            LOGGER.info("Delete notebook request received");

            Long notebookId = FrequentlyUsedMethods.getPathId(req);
            
            if (notebookId == null) {
                LOGGER.warning("Invalid notebook id provided in path");
            	sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid path: ID must be a number");
            }
            
            boolean deleted = notebookService.deleteNotebook(notebookId);
            
            if (deleted) {
                LOGGER.info("Notebook deleted successfully");
                sendSuccess(resp, "Notebook deleted successfully");
            } else {
                LOGGER.warning("Notebook not found for deletion");
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Notebook not found");
            }
        } catch (Exception e) {
            LOGGER.severe("Error while deleting notebook: " + e.getMessage());
            sendServerError(resp, e);
        }
    }
}