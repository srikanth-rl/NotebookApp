// --- State ---
let currentNotebookId = null;
let currentNoteId = null;
let currentNotebookTitle = "";
let isLoggedIn = false;
const staticCountries = {"91":"India","1":"USA / Canada / Caribbean","44":"United Kingdom","971":"United Arab Emirates","32":"Belgium","269":"Comoros","255":"Tanzania","389":"North Macedonia","966":"Saudi Arabia","421":"Slovakia","850":"North Korea","236":"Central African Republic","260":"Zambia","63":"Philippines","382":"Montenegro","56":"Chile","968":"Oman","374":"Armenia","92":"Pakistan","690":"Tokelau","229":"Benin","506":"Costa Rica","597":"Suriname","36":"Hungary","31":"Netherlands","998":"Uzbekistan","291":"Eritrea","232":"Sierra Leone","965":"Kuwait","975":"Bhutan","357":"Cyprus","33":"France","48":"Poland","43":"Austria","423":"Liechtenstein","855":"Cambodia","49":"Germany","224":"Guinea","240":"Equatorial Guinea","249":"Sudan","253":"Djibouti","45":"Denmark","46":"Sweden","58":"Venezuela","218":"Libya","502":"Guatemala","856":"Laos","880":"Bangladesh","688":"Tuvalu","355":"Albania","268":"Eswatini","967":"Yemen","244":"Angola","250":"Rwanda","243":"DR Congo","380":"Ukraine","228":"Togo","370":"Lithuania","973":"Bahrain","852":"Hong Kong","351":"Portugal","972":"Israel","239":"São Tomé and Príncipe","40":"Romania","676":"Tonga","220":"Gambia","227":"Niger","886":"Taiwan","964":"Iraq","64":"New Zealand","53":"Cuba","251":"Ethiopia","356":"Malta","261":"Madagascar","387":"Bosnia and Herzegovina","995":"Georgia","687":"New Caledonia","372":"Estonia","504":"Honduras","93":"Afghanistan","509":"Haiti","264":"Namibia","245":"Guinea-Bissau","66":"Thailand","7":"Russia / Kazakhstan","598":"Uruguay","225":"Ivory Coast","55":"Brazil","976":"Mongolia","371":"Latvia","592":"Guyana","689":"French Polynesia","507":"Panama","223":"Mali","51":"Peru","62":"Indonesia","262":"Mayotte / Réunion","373":"Moldova","963":"Syria","65":"Singapore","20":"Egypt","960":"Maldives","670":"Timor-Leste","962":"Jordan","252":"Somalia","994":"Azerbaijan","258":"Mozambique","238":"Cape Verde","359":"Bulgaria","503":"El Salvador","352":"Luxembourg","57":"Colombia","235":"Chad","90":"Turkey","501":"Belize","233":"Ghana","256":"Uganda","381":"Serbia","60":"Malaysia","683":"Niue","353":"Ireland","86":"China","385":"Croatia","248":"Seychelles","27":"South Africa","34":"Spain","675":"Papua New Guinea","41":"Switzerland","263":"Zimbabwe","386":"Slovenia","241":"Gabon","212":"Morocco","377":"Monaco","266":"Lesotho","226":"Burkina Faso","257":"Burundi","358":"Finland","992":"Tajikistan","593":"Ecuador","39":"Italy","95":"Myanmar","81":"Japan","211":"South Sudan","231":"Liberia","591":"Bolivia","685":"Samoa","84":"Vietnam","242":"Republic of the Congo","98":"Iran","961":"Lebanon","237":"Cameroon","221":"Senegal","853":"Macau","970":"Palestine","674":"Nauru","378":"San Marino","677":"Solomon Islands","691":"Micronesia","265":"Malawi","974":"Qatar","267":"Botswana","354":"Iceland","254":"Kenya","216":"Tunisia"};

// --- Initialization ---
document.addEventListener('DOMContentLoaded', function() {
    populateTimeZones('s_timezone');
    populateTimeZones('p_timezone');

    ['l_country_code', 's_country_code', 'p_add_country_code'].forEach(id => {
        const select = document.getElementById(id);
        if (select) {
            for (const code in staticCountries) {
                const opt = document.createElement('option');
                opt.value = code;
                opt.text = `+${code} (${staticCountries[code]})`;
                select.appendChild(opt);
            }
            select.value = "91";
        }
    });

    const loader = document.getElementById('app-loader');
    const hash = window.location.hash.replace('#', '');

    window.addEventListener('hashchange', handleHashChange);

    const wasLoggedIn = localStorage.getItem('isLoggedIn') === 'true';

    if (!wasLoggedIn && (hash === 'login' || hash === 'signup')) {
        isLoggedIn = false;
        handleHashChange();
        if (loader) loader.style.display = 'none';
        return;
    }

    fetch('user/profile', { credentials: 'include' })
        .then(res => {
            if (res.ok) return res.json();
            throw 'not-logged-in';
        })
        .then(res => {
            const user = res.data || res;
            isLoggedIn = true;
            localStorage.setItem('isLoggedIn', 'true');
            updateUserName(user.displayName);

            const currentHash = window.location.hash.replace('#', '');
            if (currentHash === 'login' || currentHash === 'signup' || currentHash === '') {
                window.location.hash = 'home';
            } else if (currentHash === 'profile') {
                show('profile');
                loadProfile(user);
            } else {
                handleHashChange();
            }
        })
        .catch(() => {
            isLoggedIn = false;
            localStorage.setItem('isLoggedIn', 'false');
            const currentHash = window.location.hash.replace('#', '');
            if (currentHash !== 'signup') {
                window.location.hash = 'login';
            }
            handleHashChange();
        })
        .finally(() => {
            if (loader) loader.style.display = 'none';
        });
});

// --- View & Navigation ---
function handleHashChange() {
    const hash = window.location.hash.replace('#', '');

    if (!isLoggedIn && hash !== 'login' && hash !== 'signup') {
        window.location.hash = 'login';
        return;
    }

    if (isLoggedIn && (hash === 'login' || hash === 'signup')) {
        window.location.hash = 'home';
        return;
    }

    if (hash === 'profile') {
        show('profile');
        loadProfile();
    }
    else if (hash === 'notebooks') {
        show('notebooks');
        loadNotebooks();
    }
    // notebooks/{notebookId}/notes
    else if (/^notebooks\/\d+\/notes$/.test(hash)) {
        const parts = hash.split('/');
        currentNotebookId = parts[1];
        currentNoteId = null;
        show('notes-list-view');
        loadNotesList(currentNotebookId);
    }
    // notebooks/{notebookId}/notes/{noteId}
    else if (/^notebooks\/\d+\/notes\/\d+$/.test(hash)) {
        const parts = hash.split('/');
        currentNotebookId = parts[1];
        currentNoteId = parts[3];
        show('note-detail-view');
        loadNoteDetail(currentNotebookId, currentNoteId);
    }
    else if (hash === 'login') show('login');
    else if (hash === 'signup') show('signup');
    else show('home');
}

function show(viewName) {
    document.querySelectorAll('.view').forEach(el => el.classList.add('hide'));
    const view = document.getElementById(viewName + '-view');
    if (view) view.classList.remove('hide');
}

// ==========================================
// --- Notebooks ---
// ==========================================

function loadNotebooks() {
    fetch('notebooks')
        .then(res => {
            if (res.status === 401) { handleSessionExpiry(); return null; }
            return res.ok ? res.json() : [];
        })
        .then(json => {
            if (!json) return;
            const list = json.data || json;
            const grid = document.getElementById('notebook-grid');
            if (!list || list.length === 0) {
                grid.innerHTML = '<p style="text-align:center; width:100%; color:#888;">No notebooks found.</p>';
                return;
            }
            let html = '';
            list.forEach(nb => {
                html += `
                    <div class="notebook-card" onclick="location.hash='notebooks/${nb.id}/notes'">
                        <h4>${escapeHtml(nb.title)}</h4>
                        <p>Updated: ${nb.updatedAt || 'Recently'}</p>
                        <div class="card-actions" onclick="event.stopPropagation();">
                            <button class="btn-sm btn-primary">Open</button>
                            <button class="btn-sm btn-secondary" onclick="triggerRename(event, '${nb.id}', '${escapeHtmlAttr(nb.title)}')">Rename</button>
                            <button class="btn-sm btn-danger" onclick="deleteNotebook(event, '${nb.id}')">Delete</button>
                        </div>
                    </div>
                `;
            });
            grid.innerHTML = html;
        });
}

function createNotebook() {
    const title = prompt("Enter Notebook Name:");
    if (title) {
        post('notebooks', { title }, json => {
            alert(json.message || "Notebook created successfully!");
            loadNotebooks();
        });
    }
}

function triggerRename(e, id, oldTitle) {
    if (e) e.stopPropagation();
    const newTitle = prompt("Rename Notebook:", oldTitle);
    if (newTitle && newTitle !== oldTitle) {
        fetch(`notebooks/${id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ title: newTitle })
        })
        .then(async res => {
            if (res.status === 401) { handleSessionExpiry(); return; }
            const json = await res.json().catch(() => ({}));
            if (res.ok) {
                alert(json.message || "Notebook updated successfully!");
                loadNotebooks();
            } else {
                alert(json.message || json.error || "Failed to update notebook");
            }
        });
    }
}

function deleteNotebook(e, id) {
    if (e) e.stopPropagation();
    if (confirm("Delete this notebook?")) {
        fetch(`notebooks/${id}`, { method: 'DELETE' })
        .then(async res => {
            if (res.status === 401) { handleSessionExpiry(); return; }
            const json = await res.json().catch(() => ({}));
            if (res.ok) {
                alert(json.message || "Notebook deleted successfully!");
                loadNotebooks();
            } else {
                alert(json.message || json.error || "Failed to delete notebook");
            }
        });
    }
}

// ==========================================
// --- Notes List View (notebooks/{id}/notes) ---
// ==========================================

function loadNotesList(notebookId) {
    fetch(`notebooks/${notebookId}`)
        .then(res => res.ok ? res.json() : null)
        .then(json => {
            if (json && json.data) {
                currentNotebookTitle = json.data.title;
                const el = document.getElementById('notes-list-notebook-title');
                if (el) el.innerText = currentNotebookTitle;
            }
        });

    const container = document.getElementById('notes-list-container');
    container.innerHTML = '<p>Loading notes...</p>';

    fetch(`notebooks/${notebookId}/notes`)
        .then(res => {
            if (res.status === 401) { handleSessionExpiry(); return null; }
            return res.ok ? res.json() : [];
        })
        .then(json => {
            if (!json) return;
            const list = json.data || json;
            if (!list || list.length === 0) {
                container.innerHTML = '<p style="text-align:center; color:#888;">No notes yet.</p>';
                return;
            }
            let html = '';
            list.forEach(note => {
                const pinBadge = note.pinned
                    ? '<span class="pin-badge">📌 Pinned</span>'
                    : '';
                html += `
                    <div class="note-item" onclick="location.hash='notebooks/${notebookId}/notes/${note.id}'">
                        <div class="note-header">
                            <h5>${escapeHtml(note.title) || 'Untitled'}</h5>
                            ${pinBadge}
                        </div>
                        <div class="note-meta">
                            <span class="note-date">Created: ${note.createdAt || ''}</span>
                            <span class="note-date">Updated: ${note.updatedAt || ''}</span>
                        </div>
                    </div>
                `;
            });
            container.innerHTML = html;
        });
}

function createNote() {
    document.getElementById('modal-title').innerText = 'Create New Note';
    document.getElementById('modal-note-title').value = '';
    document.getElementById('modal-note-content').value = '';
    document.getElementById('modal-note-pinned').checked = false;
    document.getElementById('modal-save-btn').onclick = saveNoteFromModal;
    document.getElementById('note-modal').classList.remove('hide');
}

function closeNoteModal() {
    document.getElementById('note-modal').classList.add('hide');
    document.getElementById('modal-note-pinned').checked = false;
}

function saveNoteFromModal() {
    const title = document.getElementById('modal-note-title').value;
    const content = document.getElementById('modal-note-content').value;
    if (!title) { alert("Title is required"); return; }

    const isPinned = document.getElementById('modal-note-pinned').checked;
    post(`notebooks/${currentNotebookId}/notes`, { title, content, isPinned }, () => {
        closeNoteModal();
        loadNotesList(currentNotebookId);
    });
}

// ==========================================
// --- Note Detail View (notebooks/{id}/notes/{noteId}) ---
// ==========================================

function loadNoteDetail(notebookId, noteId) {
    fetch(`notebooks/${notebookId}/notes/${noteId}`)
        .then(res => {
            if (res.status === 401) { handleSessionExpiry(); return null; }
            return res.ok ? res.json() : null;
        })
        .then(json => {
            if (!json || !json.data) return;
            const note = json.data;

            document.getElementById('note-detail-title').innerText = note.title || 'Untitled';
            document.getElementById('note-detail-content').innerText = note.content || '';
            document.getElementById('note-detail-created').innerText = 'Created: ' + (note.createdAt || '');
            document.getElementById('note-detail-updated').innerText = 'Updated: ' + (note.updatedAt || '');

            const pinnedEl = document.getElementById('note-detail-pinned');
            pinnedEl.dataset.pinned = note.pinned ? 'true' : 'false';
            pinnedEl.innerText = note.pinned ? '📌 Pinned' : '';
        });
}

function editNoteDetail() {
    const title = document.getElementById('note-detail-title').innerText;
    const content = document.getElementById('note-detail-content').innerText;
    const isPinned = document.getElementById('note-detail-pinned').dataset.pinned === 'true';

    document.getElementById('modal-title').innerText = 'Edit Note';
    document.getElementById('modal-note-title').value = title;
    document.getElementById('modal-note-content').value = content;
    document.getElementById('modal-note-pinned').checked = isPinned;
    document.getElementById('modal-save-btn').onclick = updateNoteFromModal;
    document.getElementById('note-modal').classList.remove('hide');
}

function updateNoteFromModal() {
    const title = document.getElementById('modal-note-title').value;
    const content = document.getElementById('modal-note-content').value;
    if (!title) { alert("Title is required"); return; }

    const isPinned = document.getElementById('modal-note-pinned').checked;
    fetch(`notebooks/${currentNotebookId}/notes/${currentNoteId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title, content, isPinned })
    })
    .then(async res => {
        if (res.status === 401) { handleSessionExpiry(); return; }
        const json = await res.json().catch(() => ({}));
        if (res.ok) {
            alert(json.message || "Note updated successfully!");
            closeNoteModal();
            loadNoteDetail(currentNotebookId, currentNoteId);
        } else {
            alert(json.message || json.error || "Failed to update note");
        }
    });
}

function deleteNoteDetail() {
    if (confirm("Delete this note?")) {
        fetch(`notebooks/${currentNotebookId}/notes/${currentNoteId}`, { method: 'DELETE' })
        .then(async res => {
            if (res.status === 401) { handleSessionExpiry(); return; }
            const json = await res.json().catch(() => ({}));
            if (res.ok) {
                alert(json.message || "Note deleted successfully!");
                window.location.hash = `notebooks/${currentNotebookId}/notes`;
            } else {
                alert(json.message || json.error || "Failed to delete note");
            }
        });
    }
}

// ==========================================
// --- Auth & Profile ---
// ==========================================

function doLogin() {
    if (isLoggedIn) { window.location.hash = 'home'; return; }
    const userInput = val('l_email');
    const data = {
        value: userInput,
        password: val('l_pass'),
        countryCode: val('l_country_code')
    };
    post('user/login', data, res => {
        isLoggedIn = true;
        localStorage.setItem('isLoggedIn', 'true');
        updateUserName((res.data && res.data.displayName) || res.displayName || '');
        if (res.message) alert(res.message);
        window.location.hash = 'home';
    });
}

function doSignup() {
    if (isLoggedIn) { window.location.hash = 'home'; return; }
    const data = {
        displayName: val('s_name'),
        email: val('s_email'),
        password: val('s_pass'),
        phoneNumber: val('s_phone'),
        countryCode: val('s_country_code'),
        timeZone: val('s_timezone')
    };
    post('user/signup', data, res => {
        alert(res.message || "Success! Please login.");
        window.location.hash = 'login';
    });
}

function doLogout() {
    fetch('user/logout').then(() => {
        isLoggedIn = false;
        localStorage.setItem('isLoggedIn', 'false');
        window.location.hash = 'login';
        location.reload();
    });
}

function loadProfile(cachedUser = null) {
    const renderProfile = user => {
        updateUserName(user.displayName);
        if (document.getElementById('p_name')) {
            document.getElementById('p_name').value = user.displayName || "";
            document.getElementById('p_timezone').value = user.timeZone || Intl.DateTimeFormat().resolvedOptions().timeZone;
        }
        fetchContacts();
    };

    if (cachedUser) {
        renderProfile(cachedUser);
    } else {
        fetch('user/profile', { credentials: 'include' })
            .then(res => res.status === 401 ? Promise.reject('session-expired') : res.json())
            .then(json => renderProfile(json.data || json))
            .catch(err => err === 'session-expired' && handleSessionExpiry());
    }
}

function fetchContacts() {
    fetch('user/contacts')
    .then(res => {
        if (res.status === 401) throw 'session-expired';
        return res.ok ? res.json() : { emails: [], phones: [] };
    })
    .then(json => {
        const contacts = json.data || json;
        renderTable('email-list', contacts.emails || [], 'email');
        renderTable('phone-list', contacts.phones || [], 'phone');
    })
    .catch(err => err === 'session-expired' && handleSessionExpiry());
}

function saveProfile() {
    const data = { displayName: val('p_name'), timeZone: val('p_timezone') };
    fetch('user/profile', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        credentials: 'include'
    })
    .then(async res => {
        if (res.status === 401) { handleSessionExpiry(); return; }
        const json = await res.json();
        if (res.ok) {
            alert(json.message || "Updated");
            updateUserName(data.displayName);
        }
    });
}

function addContact(type) {
    if (type === 'email') {
        const value = prompt("Enter Email:");
        if (value) post('user/contacts', { type: 'email', value }, () => fetchContacts());
    } else {
        document.getElementById('phone-modal').classList.remove('hide');
        document.getElementById('phone-modal').style.display = 'flex';
    }
}

function submitPhoneContact() {
    const v = val('p_add_phone_val'), c = val('p_add_country_code');
    if (v) {
        post('user/contacts', { type: 'phone', value: v, countryCode: c }, () => {
            closePhoneModal();
            fetchContacts();
        });
    }
}

function closePhoneModal() {
    document.getElementById('phone-modal').classList.add('hide');
    document.getElementById('phone-modal').style.display = 'none';
    document.getElementById('p_add_phone_val').value = '';
}

function setPrimary(type, value, countryCode) {
    if (confirm("Make primary?")) {
        const payload = { type, value, action: 'PRIMARY' };
        if (type === 'phone') payload.countryCode = countryCode;
        fetch('user/contacts', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(async res => {
            const json = await res.json();
            if (res.status === 401) { handleSessionExpiry(); return; }
            if (res.ok) fetchContacts();
            else alert(json.message || json.error || "Failed to set primary");
        });
    }
}

function deleteContact(type, value, countryCode) {
    if (confirm("Delete?")) {
        const payload = { type, value };
        if (type === 'phone') payload.countryCode = countryCode;
        fetch('user/contacts', {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
        .then(async res => {
            const json = await res.json();
            if (res.status === 401) { handleSessionExpiry(); return; }
            if (res.ok) fetchContacts();
            else alert(json.message || json.error || "Failed to delete");
        });
    }
}

// ==========================================
// --- Utils ---
// ==========================================

function handleSessionExpiry() {
    alert("Session expired. Please login again.");
    isLoggedIn = false;
    localStorage.setItem('isLoggedIn', 'false');
    window.location.hash = 'login';
}

function populateTimeZones(elementId) {
    const select = document.getElementById(elementId);
    if (!select || select.options.length > 0) return;
    Intl.supportedValuesOf('timeZone').forEach(zone => {
        const opt = document.createElement('option');
        opt.value = opt.text = zone;
        select.appendChild(opt);
    });
    select.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
}

function updateUserName(name) {
    const el = document.getElementById('welcome-name');
    if (el && name) el.innerText = name;
}

function val(id) { return document.getElementById(id).value; }

function renderTable(id, items, type) {
    const tbody = document.getElementById(id);
    if (!tbody) return;
    let html = '';
    if (!items || items.length === 0) {
        html = '<tr><td colspan="2" style="text-align:center;">No data</td></tr>';
    } else {
        items.forEach(item => {
            const rawVal = item.value || item.phoneNumber || item.email;
            const code = item.countryCode || "";
            const displayTxt = (type === 'phone' && code) ? `+${code} ${rawVal}` : rawVal;
            const isPrimary = item.primary || item.isPrimary;
            let actionHtml = isPrimary ? '<span class="primary-badge">Primary</span>' :
                `<button class="action-btn" onclick="setPrimary('${type}', '${rawVal}', '${code}')">Set Primary</button>
                 <button class="delete-btn" onclick="deleteContact('${type}', '${rawVal}', '${code}')">Delete</button>`;
            html += `<tr><td>${displayTxt}</td><td>${actionHtml}</td></tr>`;
        });
    }
    tbody.innerHTML = html;
}

function post(url, data, onSuccess) {
    fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data) })
    .then(async res => {
        const json = await res.json().catch(() => ({}));
        if (res.ok) onSuccess(json);
        else alert(json.error || json.message || "Operation failed");
    })
    .catch(err => console.error(err));
}

function escapeHtml(text) {
    if (!text) return '';
    const map = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' };
    return text.replace(/[&<>"']/g, m => map[m]);
}

function escapeHtmlAttr(text) {
    if (!text) return '';
    return text.replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

window.addEventListener('storage', event => {
    if (event.key === 'isLoggedIn') {
        if (event.newValue === 'true') {
            isLoggedIn = true;
            const hash = window.location.hash.replace('#', '');
            if (hash === 'login' || hash === 'signup') window.location.hash = 'home';
        } else {
            isLoggedIn = false;
            window.location.hash = 'login';
        }
    }
});