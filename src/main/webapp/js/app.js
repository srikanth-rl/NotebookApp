// --- State ---
let currentNotebookId = null;
let currentNoteId = null;
let currentNotebookTitle = "";
let isLoggedIn = false;
var profileLoaded = false;

// --- Initialization ---
document.addEventListener('DOMContentLoaded', function() {
    populateTimeZones('p_timezone');

    ['p_add_country_code'].forEach(function(id) {
        var select = document.getElementById(id);
        if (select) {
            var staticCountries = {"91":"India","1":"USA / Canada","44":"United Kingdom","971":"UAE","32":"Belgium","92":"Pakistan","49":"Germany","33":"France","86":"China","81":"Japan","55":"Brazil","7":"Russia","39":"Italy","34":"Spain","61":"Australia","27":"South Africa","82":"South Korea","52":"Mexico","31":"Netherlands","46":"Sweden","41":"Switzerland","43":"Austria","45":"Denmark","47":"Norway","48":"Poland","351":"Portugal","353":"Ireland","358":"Finland","372":"Estonia","371":"Latvia","370":"Lithuania","380":"Ukraine","36":"Hungary","40":"Romania","359":"Bulgaria","385":"Croatia","386":"Slovenia","421":"Slovakia","420":"Czech Republic","66":"Thailand","62":"Indonesia","60":"Malaysia","65":"Singapore","63":"Philippines","84":"Vietnam","880":"Bangladesh","94":"Sri Lanka","977":"Nepal","98":"Iran","964":"Iraq","966":"Saudi Arabia","974":"Qatar","973":"Bahrain","965":"Kuwait","968":"Oman","962":"Jordan","961":"Lebanon","972":"Israel","20":"Egypt","212":"Morocco","213":"Algeria","216":"Tunisia","218":"Libya","234":"Nigeria","254":"Kenya","233":"Ghana","256":"Uganda","255":"Tanzania","251":"Ethiopia","260":"Zambia","263":"Zimbabwe","264":"Namibia","267":"Botswana"};
            for (var code in staticCountries) {
                var opt = document.createElement('option');
                opt.value = code;
                opt.text = '+' + code + ' (' + staticCountries[code] + ')';
                select.appendChild(opt);
            }
            select.value = "91";
        }
    });

    var loader = document.getElementById('app-loader');

    window.addEventListener('hashchange', handleHashChange);

    // Verify IAM session is active by hitting a protected endpoint.
    // If IAM SecurityFilter rejects the request (401 or redirect to signin), user is not authenticated.
    fetch('user/profile', { credentials: 'include' })
        .then(function(res) {
            // res.redirected = true means IAM redirected fetch to signin page (CORS will block it)
            if (res.ok && !res.redirected) return res.json();
            throw res.redirected ? 'redirected' : 'not-authenticated';
        })
        .then(function(res) {
            var user = res.data || res;
            isLoggedIn = true;
            profileLoaded = true;
            updateUserName(user.displayName);

            var currentHash = window.location.hash.replace('#', '');
            if (currentHash === '' || currentHash === 'home') {
                window.location.hash = 'notebooks';
            } else if (currentHash === 'profile') {
                show('profile');
                loadProfile(user);
            } else {
                handleHashChange();
            }
        })
        .catch(function(err) {
            isLoggedIn = false;
            if (err instanceof TypeError) {
                // Network error = server down
                handleServerDown();
            } else {
                // Not authenticated or redirected to IAM signin — go to login.jsp
                window.location.href = 'login.jsp';
            }
        })
        .finally(function() {
            if (loader) loader.style.display = 'none';
        });
});

// --- Logout ---
// Redirect to IAM logout endpoint — IAM clears IAMAGENTTICKET cookie and redirects back
function doLogout() {
    window.location.href = 'user/logout';
}

// --- Session / Server error handlers ---
function handleSessionExpiry() {
    isLoggedIn = false;
    alert("Session expired. Please login again.");
    window.location.href = 'user/logout';
}

function handleServerDown() {
    isLoggedIn = false;
    window.location.href = 'login.jsp';
}

// Central fetch wrapper — handles 401, IAM redirects, and network errors uniformly
function safeFetch(url, options) {
    return fetch(url, options || {})
        .then(function(res) {
            // res.redirected = IAM redirected to signin page (CORS blocks it)
            if (res.status === 401 || res.redirected) {
                handleSessionExpiry();
                return Promise.reject('session-expired');
            }
            return res;
        })
        .catch(function(err) {
            if (err === 'session-expired') return Promise.reject(err);
            // TypeError = network failure = server down
            if (err instanceof TypeError) handleServerDown();
            return Promise.reject(err);
        });
}

// --- View & Navigation ---
function handleHashChange() {
    var hash = window.location.hash.replace('#', '');

    if (!isLoggedIn) {
        window.location.href = 'login.jsp';
        return;
    }

    if (hash === 'profile') {
        show('profile');
        loadProfile();
    } else if (hash === 'notebooks') {
        show('notebooks');
        loadNotebooks();
    } else if (/^notebooks\/\d+\/notes$/.test(hash)) {
        var parts = hash.split('/');
        currentNotebookId = parts[1];
        currentNoteId = null;
        show('notes-list');
        loadNotesList(currentNotebookId);
    } else if (/^notebooks\/\d+\/notes\/\d+$/.test(hash)) {
        var parts2 = hash.split('/');
        currentNotebookId = parts2[1];
        currentNoteId = parts2[3];
        show('note-detail');
        loadNoteDetail(currentNotebookId, currentNoteId);
    } else {
        show('home');
    }
}

function show(viewName) {
    document.querySelectorAll('.view').forEach(function(el) {
        el.classList.add('hide');
    });
    var view = document.getElementById(viewName + '-view');
    if (view) view.classList.remove('hide');
}

// ==========================================
// --- Notebooks ---
// ==========================================

function loadNotebooks() {
    safeFetch('notebooks')
        .then(function(res) {
            return res.ok ? res.json() : [];
        })
        .then(function(json) {
            if (!json) return;
            var list = json.data || json;
            var grid = document.getElementById('notebook-grid');
            if (!list || list.length === 0) {
                grid.innerHTML = '<p style="text-align:center; width:100%; color:#888;">No notebooks found.</p>';
                return;
            }
            var html = '';
            list.forEach(function(nb) {
                html += '<div class="notebook-card" onclick="location.hash=\'notebooks/' + nb.id + '/notes\'">' +
                    '<h4>' + escapeHtml(nb.title) + '</h4>' +
                    '<p>Updated: ' + (nb.updatedAt || 'Recently') + '</p>' +
                    '<div class="card-actions" onclick="event.stopPropagation();">' +
                    '<button class="btn-sm btn-primary">Open</button>' +
                    '<button class="btn-sm btn-secondary" onclick="triggerRename(event,\'' + nb.id + '\',\'' + escapeHtmlAttr(nb.title) + '\')">Rename</button>' +
                    '<button class="btn-sm btn-danger" onclick="deleteNotebook(event,\'' + nb.id + '\')">Delete</button>' +
                    '</div></div>';
            });
            grid.innerHTML = html;
        })
        .catch(function() {});
}

function createNotebook() {
    var title = prompt("Enter Notebook Name:");
    if (title) {
        safePost('notebooks', { title: title }, function(json) {
            alert(json.message || "Notebook created successfully!");
            loadNotebooks();
        });
    }
}

function triggerRename(e, id, oldTitle) {
    if (e) e.stopPropagation();
    var newTitle = prompt("Rename Notebook:", oldTitle);
    if (newTitle && newTitle !== oldTitle) {
        safeFetch('notebooks/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title: newTitle })
        })
        .then(function(res) {
            return res.json().catch(function() { return {}; }).then(function(json) {
                if (res.ok) {
                    alert(json.message || "Notebook updated successfully!");
                    loadNotebooks();
                } else {
                    alert(json.message || json.error || "Failed to update notebook");
                }
            });
        })
        .catch(function() {});
    }
}

function deleteNotebook(e, id) {
    if (e) e.stopPropagation();
    if (confirm("Delete this notebook?")) {
        safeFetch('notebooks/' + id, { method: 'DELETE' })
        .then(function(res) {
            return res.json().catch(function() { return {}; }).then(function(json) {
                if (res.ok) {
                    alert(json.message || "Notebook deleted successfully!");
                    loadNotebooks();
                } else {
                    alert(json.message || json.error || "Failed to delete notebook");
                }
            });
        })
        .catch(function() {});
    }
}

// ==========================================
// --- Notes List ---
// ==========================================

function loadNotesList(notebookId) {
    safeFetch('notebooks/' + notebookId)
        .then(function(res) { return res.ok ? res.json() : null; })
        .then(function(json) {
            if (json && json.data) {
                currentNotebookTitle = json.data.title;
                var el = document.getElementById('notes-list-notebook-title');
                if (el) el.innerText = currentNotebookTitle;
            }
        })
        .catch(function() {});

    var container = document.getElementById('notes-list-container');
    container.innerHTML = '<p>Loading notes...</p>';

    safeFetch('notebooks/' + notebookId + '/notes')
        .then(function(res) {
            return res.ok ? res.json() : [];
        })
        .then(function(json) {
            if (!json) return;
            var list = json.data || json;
            if (!list || list.length === 0) {
                container.innerHTML = '<p style="text-align:center; color:#888;">No notes found.</p>';
                return;
            }
            var html = '';
            list.forEach(function(note) {
                html += buildNoteCard(note);
            });
            container.innerHTML = html;
        })
        .catch(function() {});
}

function buildNoteCard(note) {
    var pinned = note.pinned ? '<span class="pin-badge">📌 Pinned</span>' : '';
    return '<div class="note-item" id="note-' + note.id + '">' +
        '<div class="note-header" onclick="toggleNoteExpand(' + note.id + ',' + currentNotebookId + ')">' +
        '<h5>' + escapeHtml(note.title) + '</h5>' +
        pinned +
        '<span class="note-date">' + (note.updatedAt || '') + '</span>' +
        '</div>' +
        '<p class="note-preview" id="preview-' + note.id + '">' + escapeHtml((note.content || '').substring(0, 120)) + '</p>' +
        '<div class="note-actions">' +
        '<button class="btn-edit btn-sm" onclick="openNoteEditInline(' + note.id + ',' + currentNotebookId + ')">Edit</button>' +
        '<button class="btn-sm btn-danger" onclick="deleteNoteInline(' + note.id + ',' + currentNotebookId + ')">Delete</button>' +
        '</div>' +
        '</div>';
}

function toggleNoteExpand(noteId, notebookId) {
    location.hash = 'notebooks/' + notebookId + '/notes/' + noteId;
}

function loadNoteDetail(notebookId, noteId) {
    safeFetch('notebooks/' + notebookId + '/notes/' + noteId)
        .then(function(res) {
            return res.ok ? res.json() : null;
        })
        .then(function(json) {
            if (!json) return;
            var note = json.data || json;
            document.getElementById('note-detail-title').innerText = note.title || '';
            document.getElementById('note-detail-content').innerText = note.content || '';
            document.getElementById('note-detail-pinned').innerText = note.pinned ? '📌 Pinned' : '';
            document.getElementById('note-detail-created').innerText = note.createdAt ? 'Created: ' + note.createdAt : '';
            document.getElementById('note-detail-updated').innerText = note.updatedAt ? 'Updated: ' + note.updatedAt : '';
        })
        .catch(function() {});
}

function createNote() {
    openNoteModal(null, null);
}

function openNoteModal(noteId, notebookId) {
    var modal = document.getElementById('note-modal');
    document.getElementById('modal-title').innerText = noteId ? 'Edit Note' : 'Create New Note';
    document.getElementById('modal-note-title').value = '';
    document.getElementById('modal-note-content').value = '';
    document.getElementById('modal-note-pinned').checked = false;

    if (noteId) {
        safeFetch('notebooks/' + notebookId + '/notes/' + noteId)
            .then(function(res) { return res.ok ? res.json() : null; })
            .then(function(json) {
                if (!json) return;
                var note = json.data || json;
                document.getElementById('modal-note-title').value = note.title || '';
                document.getElementById('modal-note-content').value = note.content || '';
                document.getElementById('modal-note-pinned').checked = !!note.pinned;
            })
            .catch(function() {});
    }

    modal.classList.remove('hide');
    document.getElementById('modal-save-btn').onclick = function() {
        saveNote(noteId, notebookId || currentNotebookId);
    };
}

function closeNoteModal() {
    document.getElementById('note-modal').classList.add('hide');
}

function saveNote(noteId, notebookId) {
    var payload = {
        title: document.getElementById('modal-note-title').value,
        content: document.getElementById('modal-note-content').value,
        pinned: document.getElementById('modal-note-pinned').checked
    };
    var url = 'notebooks/' + notebookId + '/notes' + (noteId ? '/' + noteId : '');
    var method = noteId ? 'PUT' : 'POST';

    safeFetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    })
    .then(function(res) {
        return res.json().then(function(json) {
            if (res.ok) {
                closeNoteModal();
                loadNotesList(notebookId);
            } else {
                alert(json.message || json.error || "Operation failed");
            }
        });
    })
    .catch(function() {});
}

function openNoteEditInline(noteId, notebookId) {
    openNoteModal(noteId, notebookId);
}

function deleteNoteInline(noteId, notebookId) {
    if (confirm("Delete this note?")) {
        safeFetch('notebooks/' + notebookId + '/notes/' + noteId, { method: 'DELETE' })
        .then(function(res) {
            return res.json().then(function(json) {
                if (res.ok) loadNotesList(notebookId);
                else alert(json.message || "Failed to delete note");
            });
        })
        .catch(function() {});
    }
}

function editNoteDetail() {
    openNoteModal(currentNoteId, currentNotebookId);
}

function deleteNoteDetail() {
    if (confirm("Delete this note?")) {
        safeFetch('notebooks/' + currentNotebookId + '/notes/' + currentNoteId, { method: 'DELETE' })
        .then(function(res) {
            return res.json().then(function(json) {
                if (res.ok) location.hash = 'notebooks/' + currentNotebookId + '/notes';
                else alert(json.message || "Failed to delete note");
            });
        })
        .catch(function() {});
    }
}

// ==========================================
// --- Profile ---
// ==========================================

var cachedUser = null;

function loadProfile(user) {
    var renderProfile = function(u) {
        cachedUser = u;
        if (document.getElementById('p_name')) {
            document.getElementById('p_name').value = u.displayName || '';
            document.getElementById('p_timezone').value = u.timezone ||
                Intl.DateTimeFormat().resolvedOptions().timeZone;
        }
        fetchContacts();
    };

    if (user) {
        renderProfile(user);
        return;
    }

    if (cachedUser) {
        renderProfile(cachedUser);
        return;
    }

    safeFetch('user/profile', { credentials: 'include' })
        .then(function(res) { return res.json(); })
        .then(function(json) {
            if (json) renderProfile(json.data || json);
        })
        .catch(function() {});
}

function fetchContacts() {
    safeFetch('user/contacts')
    .then(function(res) {
        return res.ok ? res.json() : { emails: [], phones: [] };
    })
    .then(function(json) {
        if (!json) return;
        var contacts = json.data || json;
        renderTable('email-list', contacts.emails || [], 'email');
        renderTable('phone-list', contacts.phones || [], 'phone');
    })
    .catch(function() {});
}

function saveProfile() {
    var data = { displayName: val('p_name'), timezone: val('p_timezone') };
    safeFetch('user/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        credentials: 'include'
    })
    .then(function(res) {
        return res.json().then(function(json) {
            if (res.ok) {
                alert(json.message || "Updated");
                cachedUser = null;
                updateUserName(data.displayName);
            }
        });
    })
    .catch(function() {});
}

function addContact(type) {
    if (type === 'email') {
        var value = prompt("Enter Email:");
        if (value) safePost('user/contacts', { type: 'email', value: value }, function() { fetchContacts(); });
    } else {
        var modal = document.getElementById('phone-modal');
        modal.classList.remove('hide');
        modal.style.display = 'flex';
    }
}

function submitPhoneContact() {
    var v = val('p_add_phone_val');
    var c = val('p_add_country_code');
    if (v) {
        safePost('user/contacts', { type: 'phone', value: v, countryCode: c }, function() {
            closePhoneModal();
            fetchContacts();
        });
    }
}

function closePhoneModal() {
    var modal = document.getElementById('phone-modal');
    modal.classList.add('hide');
    modal.style.display = 'none';
    document.getElementById('p_add_phone_val').value = '';
}

function setPrimary(type, value, countryCode) {
    if (confirm("Make primary?")) {
        var payload = { type: type, value: value, action: 'PRIMARY' };
        if (type === 'phone') payload.countryCode = countryCode;
        safeFetch('user/contacts', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(function(res) {
            return res.json().then(function(json) {
                if (res.ok) fetchContacts();
                else alert(json.message || json.error || "Failed to set primary");
            });
        })
        .catch(function() {});
    }
}

function deleteContact(type, value, countryCode) {
    if (confirm("Delete?")) {
        var payload = { type: type, value: value };
        if (type === 'phone') payload.countryCode = countryCode;
        safeFetch('user/contacts', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(function(res) {
            return res.json().then(function(json) {
                if (res.ok) fetchContacts();
                else alert(json.message || json.error || "Failed to delete");
            });
        })
        .catch(function() {});
    }
}

// ==========================================
// --- Utils ---
// ==========================================

function populateTimeZones(elementId) {
    var select = document.getElementById(elementId);
    if (!select || select.options.length > 0) return;
    Intl.supportedValuesOf('timeZone').forEach(function(zone) {
        var opt = document.createElement('option');
        opt.value = zone;
        opt.text = zone;
        select.appendChild(opt);
    });
    select.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
}

function updateUserName(name) {
    var el = document.getElementById('welcome-name');
    if (el && name) el.innerText = name;
}

function val(id) { return document.getElementById(id).value; }

function renderTable(id, items, type) {
    var tbody = document.getElementById(id);
    if (!tbody) return;
    var html = '';
    if (!items || items.length === 0) {
        html = '<tr><td colspan="2" style="text-align:center;">No data</td></tr>';
    } else {
        items.forEach(function(item) {
            var rawVal = item.value || item.phoneNumber || item.email;
            var code = item.countryCode || "";
            var displayTxt = (type === 'phone' && code) ? '+' + code + ' ' + rawVal : rawVal;
            var isPrimary = item.primary || item.isPrimary;
            var actionHtml = isPrimary
                ? '<span class="primary-badge">Primary</span>'
                : '<button class="action-btn" onclick="setPrimary(\'' + type + '\',\'' + rawVal + '\',\'' + code + '\')">Set Primary</button>' +
                  '<button class="delete-btn" onclick="deleteContact(\'' + type + '\',\'' + rawVal + '\',\'' + code + '\')">Delete</button>';
            html += '<tr><td>' + displayTxt + '</td><td>' + actionHtml + '</td></tr>';
        });
    }
    tbody.innerHTML = html;
}

// Safe POST wrapper — handles 401 and network errors
function safePost(url, data, onSuccess) {
    safeFetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data)
    })
    .then(function(res) {
        return res.json().catch(function() { return {}; }).then(function(json) {
            if (res.ok) onSuccess(json);
            else alert(json.error || json.message || "Operation failed");
        });
    })
    .catch(function() {});
}

function escapeHtml(text) {
    if (!text) return '';
    var map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

function escapeHtmlAttr(text) {
    if (!text) return '';
    return text.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}