package com.zoho.notebook.service;

import com.adventnet.collaboration.Collaboration;
import com.adventnet.mfw.bean.BeanUtil;
import com.adventnet.persistence.DataAccess;
import javax.transaction.TransactionManager;

import com.zoho.notebook.dao.NotebookDao;
import com.zoho.notebook.enums.OwnerType;
import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.cache.LRUCacheMap;
import com.zoho.notebook.context.UserSessionContext;
import com.zoho.notebook.dto.request.NotebookDtoRequest;

import java.util.List;
import java.util.logging.Logger;

public class NotebookService {

    private final NotebookDao notebookDao = new NotebookDao();
    private static final LRUCacheMap<Long, NotebookModel> notebookCache = new LRUCacheMap<>(500);
    private static final Logger LOGGER = Logger.getLogger(NotebookService.class.getName());

    public boolean createNotebook(NotebookDtoRequest req) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean created = false;

        try {
            txMgr.begin();

            NotebookModel nb = new NotebookModel();
            Long userId = UserSessionContext.getId();

            nb.setTitle(req.getTitle());
            nb.setOwnerType(OwnerType.USER.name());
            nb.setOwnerId(userId);
            nb.setCreatedBy(userId);
            nb.setUpdatedBy(userId);

            Long generatedNbId = notebookDao.insert(nb);

            if (generatedNbId != null && generatedNbId > 0) {
                // Reserve dataspace for this notebook after DB inserts
                String dataspaceName = "notebook_" + generatedNbId; // notebook_{id}
                Collaboration collaboration = (Collaboration) BeanUtil.lookup("CollaborationBean");
                if (!collaboration.dataSpaceExists(dataspaceName)) {
                    collaboration.reserveDataSpace(dataspaceName);
                    LOGGER.info("Dataspace reserved: " + dataspaceName);
                } else {
                    LOGGER.warning("Dataspace already exists: " + dataspaceName);
                }

                txMgr.commit();
                created = true;
                nb.setId(generatedNbId);
                notebookCache.put(generatedNbId, nb);
                LOGGER.info("Notebook created successfully. notebookId=" + generatedNbId + ", userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook creation failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return created;
    }

    public NotebookModel getNotebookById(long notebookId) throws Exception {
        NotebookModel cachedNb = notebookCache.get(notebookId);
        if (cachedNb != null) {
            return cachedNb;
        }

        return notebookDao.getNotebookById(UserSessionContext.getId(), notebookId);
    }

    public List<NotebookModel> getUserNotebooks() throws Exception {
        return notebookDao.getAllNotebooksByOwner(UserSessionContext.getId(), OwnerType.USER, UserSessionContext.getTimezone());
    }

    public boolean updateNotebook(long notebookId, NotebookDtoRequest req) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean updated = false;

        try {
            txMgr.begin();

            String title  = req.getTitle();
            Long   userId = UserSessionContext.getId();
            NotebookModel nb = notebookDao.getNotebookById(userId, notebookId);

            if (nb == null || !nb.getOwnerId().equals(userId)) {
                LOGGER.warning("Unauthorized notebook update attempt. notebookId=" + notebookId + ", userId=" + userId);
                txMgr.rollback();
                return false;
            }

            updated = notebookDao.update(notebookId, title, userId);

            if (updated) {
                txMgr.commit();
                notebookCache.remove(notebookId);
                LOGGER.info("Notebook updated successfully. notebookId=" + notebookId + ", userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook update failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return updated;
    }

    public boolean deleteNotebook(long notebookId) throws Exception {
        TransactionManager txMgr = DataAccess.getTransactionManager();
        boolean deleted = false;

        try {
            txMgr.begin();

            Long userId = UserSessionContext.getId();
            NotebookModel nb = notebookDao.getNotebookById(userId, notebookId);

            if (nb == null || !nb.getOwnerId().equals(userId)) {
                LOGGER.warning("Unauthorized notebook delete attempt. notebookId=" + notebookId + ", userId=" + userId);
                txMgr.rollback();
                return false;
            }

            deleted = notebookDao.delete(notebookId);

            if (deleted) {
                // Unlink dataspace after deletion of notebook in DB
                String dataspaceName = "notebook_" + notebookId;
                Collaboration collaboration = (Collaboration) BeanUtil.lookup("CollaborationBean");
                collaboration.unlinkDataSpace(dataspaceName);
                LOGGER.info("Dataspace unlinked: " + dataspaceName);

                txMgr.commit();
                notebookCache.remove(notebookId);
                LOGGER.info("Notebook deleted successfully. notebookId=" + notebookId + ", userId=" + userId);
            } else {
                txMgr.rollback();
            }
        } catch (Exception e) {
            LOGGER.severe("Notebook delete failed: " + e.getMessage());
            txMgr.rollback();
            throw e;
        }

        return deleted;
    }
}