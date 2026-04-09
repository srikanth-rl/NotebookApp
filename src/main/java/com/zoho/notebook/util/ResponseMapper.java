package com.zoho.notebook.util;

import java.util.List;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import com.zoho.notebook.model.UserModel;
import com.zoho.notebook.model.NoteModel;
import com.zoho.notebook.model.NotebookModel;
import com.zoho.notebook.model.UserEmailModel;
import com.zoho.notebook.model.UserPhoneModel;
import com.zoho.notebook.dto.response.UserDtoResponse;
import com.zoho.notebook.dto.response.NoteDtoResponse;
import com.zoho.notebook.dto.response.NotebookDtoResponse;
import com.zoho.notebook.dto.response.UserDtoResponse.ContactDetail;

public class ResponseMapper {

    private ResponseMapper() {}
    public static String resultSetToJson(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        StringBuilder json = new StringBuilder("{");
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String key = meta.getColumnLabel(i);
            Object value = rs.getObject(i);
            json.append("\"").append(key).append("\":");
            if (value == null) {
                json.append("null");
            } else {
                String val = value.toString().replace("\"", "\\\""); // Escape quotes
                json.append("\"").append(val).append("\"");
            }
            json.append(",");
        }
        if (json.length() > 1) json.setLength(json.length() - 1);
        return json.append("}").toString();
    }
    
    public static UserDtoResponse toLoginResponse(UserModel user) {
        if (user == null) {
            return null;
        }

        UserDtoResponse response = new UserDtoResponse();
        response.setDisplayName(user.getDisplayName());
        
        return response;
    }

    public static UserDtoResponse toProfileResponse(UserModel user) {
        if (user == null) {
            return null;
        }

        UserDtoResponse response = new UserDtoResponse();
        response.setDisplayName(user.getDisplayName());
        response.setStatus(user.getStatus());
        response.setTimezone(user.getTimezone());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    public static UserDtoResponse toContactResponse(List<UserEmailModel> emails, List<UserPhoneModel> phones) {
        UserDtoResponse response = new UserDtoResponse();

        List<ContactDetail> emailList = new ArrayList<>();
        if (emails != null) {
            for (UserEmailModel email : emails) {
                emailList.add(new ContactDetail(email.getEmail(), null, email.isPrimary()));
            }
        }

        List<ContactDetail> phoneList = new ArrayList<>();
        if (phones != null) {
            for (UserPhoneModel phone : phones) {
                phoneList.add(new ContactDetail(phone.getPhoneNumber(), phone.getCountryCode(), phone.isPrimary()));
            }
        }

        response.setEmails(emailList);
        response.setPhones(phoneList);
        
        return response;
    }
    
    
    public static NotebookDtoResponse toNotebookResponse(NotebookModel notebook) {
        if (notebook == null) {
            return null;
        }

        NotebookDtoResponse response = new NotebookDtoResponse();
        response.setId(notebook.getId());
        response.setTitle(notebook.getTitle());
        
        return response;
    }

    public static List<NotebookDtoResponse> toNotebookListResponse(List<NotebookModel> notebooks) {
        if (notebooks == null) {
            return new ArrayList<>();
        }

        List<NotebookDtoResponse> list = new ArrayList<>();
        for (NotebookModel nb : notebooks) {
        	NotebookDtoResponse singleNotebookResponse = toNotebookResponse(nb);
        	singleNotebookResponse.setCreatedAt(nb.getCreatedAt());
        	singleNotebookResponse.setUpdatedAt(nb.getUpdatedAt());
        	singleNotebookResponse.setOwnerType(nb.getOwnerType());
        	
            list.add(singleNotebookResponse);
        }

        return list;
    }
    
    public static NoteDtoResponse toNoteResponse(NoteModel note) {
        if (note == null)
            return null;

        NoteDtoResponse response = new NoteDtoResponse();
        response.setId(note.getId());
        response.setNotebookId(note.getNotebookId());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setPinned(note.isPinned());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }

    public static List<NoteDtoResponse> toNoteListResponse(List<NoteModel> notes) {
        if (notes == null)
            return new ArrayList<>();

        List<NoteDtoResponse> list = new ArrayList<>();
        for (NoteModel note : notes) {
            list.add(toNoteResponse(note));
        }
        return list;
    }
}