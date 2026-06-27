// ===== API 工具 =====
const API_BASE = '';

const api = {
    getToken() { return localStorage.getItem('token'); },
    getUser() {
        try { return JSON.parse(localStorage.getItem('user') || 'null'); }
        catch { return null; }
    },
    setToken(token) { localStorage.setItem('token', token); },
    setUser(user) { localStorage.setItem('user', JSON.stringify(user)); },
    clearAuth() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
    },
    isLoggedIn() { return !!this.getToken(); },
    isAdmin() {
        const u = this.getUser();
        return u && (u.role === 'ADMIN' || u.role === 'SUPER_ADMIN');
    },

    // 通用请求
    async request(method, path, body) {
        const headers = { 'Content-Type': 'application/json' };
        const token = this.getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;

        const options = { method, headers };
        if (body !== undefined) options.body = JSON.stringify(body);

        const res = await fetch(API_BASE + path, options);
        const json = await res.json();

        if (json.code === 401 || json.code === 9002 || json.code === 9003) {
            this.clearAuth();
            if (!window.location.pathname.includes('index.html')) {
                window.location.href = '/index.html';
            }
            throw new Error(json.message);
        }
        if (json.code === 1008) {
            // 超级管理员强制改密，特殊处理
            const err = new Error(json.message);
            err.code = json.code;
            err.data = json.data;
            throw err;
        }
        if (json.code !== 200) {
            throw new Error(json.message);
        }
        return json.data;
    },

    get(path) { return this.request('GET', path); },
    post(path, body) { return this.request('POST', path, body); },
    put(path, body) { return this.request('PUT', path, body); },
    del(path) { return this.request('DELETE', path); }
};

// ===== 提示工具 =====
function showToast(message, type = 'info') {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.className = 'toast toast-' + type;
    toast.style.display = 'block';

    clearTimeout(toast._hideTimer);
    toast._hideTimer = setTimeout(() => { toast.style.display = 'none'; }, 3000);
}

// ===== 加载用户信息 =====
async function loadNavbar() {
    const user = api.getUser();
    if (!user) return;

    const navbarUser = document.getElementById('navbarUser');
    const adminLink = document.getElementById('adminLink');
    if (navbarUser) navbarUser.textContent = user.nickname;
    if (adminLink) adminLink.style.display = (user.role === 'ADMIN' || user.role === 'SUPER_ADMIN') ? '' : 'none';
}

// ===== 获取 SockJS 连接 =====
function createStompClient() {
    const token = api.getToken();
    if (!token) return null;

    const socket = new SockJS('/ws?token=' + token);
    const client = Stomp.over(socket);
    client.reconnect_delay = 3000;
    return client;
}

// ===== 深拷贝 =====
function clone(obj) {
    return JSON.parse(JSON.stringify(obj));
}
