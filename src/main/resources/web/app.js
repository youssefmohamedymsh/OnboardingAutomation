// Shared helpers for the Ledgerline demo pages.
const SESSION_KEY = 'ledgerline.session';

const Ledgerline = {
  session() {
    return sessionStorage.getItem(SESSION_KEY);
  },

  setSession(token) {
    sessionStorage.setItem(SESSION_KEY, token);
  },

  /** Redirects to sign-up when there's no session. Use on pages behind login. */
  requireSession() {
    if (!this.session()) {
      window.location.href = '/index.html';
      throw new Error('No session');
    }
  },

  async api(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    if (this.session()) headers.Authorization = 'Bearer ' + this.session();
    const res = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      const err = new Error(data.message || 'Something went wrong');
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  },

  clearErrors(root = document) {
    root.querySelectorAll('[data-testid^="error-"], [data-testid="form-error"]').forEach((el) => {
      el.textContent = '';
    });
  },

  /** Puts each field error next to its field; anything else goes in the form-level alert. */
  showErrors(err) {
    const fieldErrors = (err.data && err.data.errors) || {};
    let placed = 0;
    for (const [field, message] of Object.entries(fieldErrors)) {
      const el = document.querySelector(`[data-testid="error-${field}"]`);
      if (el) {
        el.textContent = message;
        placed++;
      }
    }
    const formError = document.querySelector('[data-testid="form-error"]');
    if (formError && (placed === 0 || placed < Object.keys(fieldErrors).length)) {
      const unplaced = Object.entries(fieldErrors)
        .filter(([field]) => !document.querySelector(`[data-testid="error-${field}"]`))
        .map(([, m]) => m);
      formError.textContent = unplaced.length ? unplaced.join(' ') : err.message;
    }
  },

  /** Disables the button while the action runs, so double clicks don't submit twice. */
  async busy(button, action) {
    button.disabled = true;
    try {
      return await action();
    } finally {
      button.disabled = false;
    }
  },

  renderStepper(current, completed = {}) {
    const steps = [
      ['business', 'Business'],
      ['owners', 'Owners'],
      ['document', 'ID document'],
      ['review', 'Review'],
    ];
    const list = document.querySelector('[data-testid="stepper"]');
    if (!list) return;
    list.innerHTML = '';
    for (const [key, label] of steps) {
      const li = document.createElement('li');
      li.textContent = label;
      li.dataset.step = key;
      if (key === current) li.className = 'current';
      else if (completed[key]) li.className = 'done';
      list.appendChild(li);
    }
  },

  readFileAsBase64(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result).split(',')[1] || '');
      reader.onerror = () => reject(reader.error);
      reader.readAsDataURL(file);
    });
  },

  readFileAsText(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => resolve(String(reader.result));
      reader.onerror = () => reject(reader.error);
      reader.readAsText(file);
    });
  },
};
