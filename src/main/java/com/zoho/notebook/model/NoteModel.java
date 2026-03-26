package com.zoho.notebook.model;

public class NoteModel {

    private Long id;
    private Long notebookId;
    private String title;
    private String content;
    private boolean isPinned;
    private Long createdBy;
    private Long updatedBy;
    private String createdAt;
    private String updatedAt;

    public NoteModel() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getNotebookId() { return notebookId; }
    public void setNotebookId(Long notebookId) { this.notebookId = notebookId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean isPinned) { this.isPinned = isPinned; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
}