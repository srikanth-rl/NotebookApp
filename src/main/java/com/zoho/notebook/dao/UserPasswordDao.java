package com.zoho.notebook.dao;

import com.adventnet.persistence.DataAccess;
import com.adventnet.persistence.DataObject;
import com.adventnet.persistence.Row;
import com.adventnet.persistence.WritableDataObject;

import com.zoho.notebook.enums.TableName;

public class UserPasswordDao {

    public boolean insert(long userId, String pwd) throws Exception {
        long now = System.currentTimeMillis();

        Row r = new Row(TableName.UserPassword.name());
        r.set("USER_ID",       userId);
        r.set("PASSWORD_HASH", pwd);
        r.set("CREATED_AT",    now);
        r.set("UPDATED_AT",    now);
        // USER_ID is the PK — mirrors User.ID, no sequence generator

        DataObject d = new WritableDataObject();
        d.addRow(r);
        DataAccess.add(d);
        return d.getRow(TableName.UserPassword.name()) != null;
    }
}
