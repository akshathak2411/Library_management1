import './style.css';

const API = '/api';
const app = document.querySelector('#app');
let user = null;
let active = 'dashboard';

async function request(path, options = {}) {
  let response;
  try {
    const headers = { Accept: 'application/json', ...(options.headers || {}) };
    if (options.body !== undefined && !headers['Content-Type'] && !headers['content-type'])
      headers['Content-Type'] = 'application/json';
    response = await fetch(API + path, { ...options, credentials: 'include', headers });
  } catch (error) {
    if (error instanceof TypeError)
      throw new Error('Cannot reach the library backend. Restart it from the backend folder, then try again.');
    throw error;
  }
  const contentType = response.headers.get('content-type') || '';
  if (response.status !== 204 && !contentType.includes('json')) {
    if (response.ok)
      throw new Error('The API proxy returned a web page instead of data. Restart the Vite frontend and Java backend, then reload.');
    throw new Error(`The library API returned an invalid response (${response.status}).`);
  }
  const data = response.status === 204 ? {} : await response.json();
  if (!response.ok) throw new Error(data.detail || data.message || data.title || data.error || `Request failed (${response.status})`);
  return data;
}

function loginScreen(message = '') {
  app.innerHTML = `
    <div class="login-page">
      <div class="login-brand"><div class="logo large">L</div><p class="eyebrow">CAMPUS LIBRARY PORTAL</p>
        <h1>Welcome to your library</h1><p>Sign in through the correct portal to continue.</p></div>
      <div class="login-grid">
        <section class="login-card"><div class="login-icon">♙</div><p class="eyebrow">STUDENT PORTAL</p><h2>Student Login</h2>
          <p class="login-desc">View the book collection and your borrowing history.</p>
          <form id="studentLogin" class="login-form"><label>Student ID</label><input name="username" placeholder="Enter student ID" required autocomplete="username">
            <label>Password</label><input name="password" type="password" placeholder="Enter password" required autocomplete="current-password">
            <button class="primary full">Login as Student</button></form>
          <div class="demo">Enter the Student ID assigned by your admin, such as <b>stu1</b>. Demo: <b>stu1</b> / <b>Student@123</b>.</div></section>
        <section class="login-card admin-card"><div class="login-icon">▣</div><p class="eyebrow">LIBRARY ADMIN PORTAL</p><h2>Library Admin Login</h2>
          <p class="login-desc">Manage books, students, issues, returns and reports.</p>
          <form id="adminLogin" class="login-form"><label>Username</label><input name="username" placeholder="Admin" required autocomplete="username">
            <label>Password</label><input name="password" type="password" placeholder="Enter password" required autocomplete="current-password">
            <button class="primary full">Login as Admin</button></form>
          <div class="demo">Admin login: <b>Admin</b> / <b>admin@123</b></div></section>
      </div>
      <p id="loginMessage" class="login-message">${message || 'Choose the login portal that matches your account.'}</p>
      <p class="login-foot">Library Book Management and Reading Pattern Analysis System</p>
    </div>`;
  for (const [id, role] of [['studentLogin', 'STUDENT'], ['adminLogin', 'ADMIN']]) {
    document.getElementById(id).onsubmit = async e => {
      e.preventDefault();
      const f = new FormData(e.target);
      const btn = e.target.querySelector('button'); btn.disabled = true; btn.textContent = 'Signing in…';
      try {
        user = await request('/auth/login', { method: 'POST', body: JSON.stringify({ username: f.get('username'), password: f.get('password'), role }) });
        active = role === 'ADMIN' ? 'dashboard' : 'student-home';
        renderApp();
      } catch (err) {
        const m = document.querySelector('#loginMessage'); if (m) m.textContent = err.message;
        btn.disabled = false; btn.textContent = role === 'ADMIN' ? 'Login as Admin' : 'Login as Student';
      }
    };
  }
}

function table(headers, rows) {
  return `<div class="table-wrap"><table><thead><tr>${headers.map(h => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows || `<tr><td colspan="${headers.length}" class="empty">No records found</td></tr>`}</tbody></table></div>`;
}
function notify(message, error = false) {
  const box = document.querySelector('#notice'); if (box) { box.textContent = message; box.className = error ? 'notice error' : 'notice'; }
}
function shell(title, subtitle, body) {
  const admin = user.role === 'ADMIN';
  const nav = admin ? [['dashboard','▦','Dashboard'],['books','▤','Books'],['students','♙','Students'],['loans','⇄','Issue / Return'],['analytics','◒','Reading Analysis']] : [['student-home','▦','My Library']];
  app.innerHTML = `<header><div class="brand"><span class="logo">L</span><div><b>Campus Library</b><small>${admin ? 'Library Admin Workspace' : 'Student Portal'}</small></div></div>
    <div class="header-user"><span class="user-label">${admin ? 'Library Admin' : 'Student'}: <b>${user.username}</b></span><button class="secondary" id="logout">Logout</button></div></header>
    <div class="layout"><aside><p class="nav-title">${admin ? 'ADMIN WORKSPACE' : 'STUDENT WORKSPACE'}</p>${nav.map(([page,icon,label])=>`<button class="nav ${active===page?'active':''}" data-page="${page}">${icon} &nbsp; ${label}</button>`).join('')}
      <div class="side-note"><b>${admin ? 'Administrator access' : 'Student access'}</b><p>${admin ? 'Manage books, members and borrowing transactions.' : 'Browse books and track your own borrowed books.'}</p></div></aside>
      <main id="content"><div class="loading">Loading library data…</div></main></div><footer>Campus Library Management System · Java + H2 + Python</footer>`;
  document.getElementById('logout').onclick = async () => { try { await request('/auth/logout', { method: 'POST' }); } catch {} user = null; loginScreen('You have logged out.'); };
  document.querySelectorAll('.nav').forEach(btn => btn.onclick = () => { active = btn.dataset.page; renderPage(); });
}
const content = () => document.querySelector('#content');
function pageHead(title, subtitle) { return `<div class="page-head"><div><p class="eyebrow">${user.role === 'ADMIN' ? 'LIBRARY ADMIN' : 'STUDENT PORTAL'}</p><h1>${title}</h1><p>${subtitle}</p></div><span class="date">${new Date().toLocaleDateString()}</span></div><div id="notice"></div>`; }

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
}
function chartEmpty(message = 'No borrowing data for this selection.') {
  return `<div class="chart-empty">${escapeHtml(message)}</div>`;
}
function studentBarChart(rows) {
  const data = rows.filter(row => row.totalBorrowed > 0);
  if (!data.length) return chartEmpty();
  const width = Math.max(680, data.length * 64);
  const max = Math.max(1, ...data.map(row => row.totalBorrowed));
  const bars = data.map((row, i) => {
    const x = 38 + i * (width - 72) / data.length;
    const barHeight = row.totalBorrowed / max * 155;
    return `<rect x="${x}" y="${190 - barHeight}" width="34" height="${barHeight}" rx="6" fill="#315b91"><title>${escapeHtml(row.studentName)}: ${row.totalBorrowed}</title></rect><text x="${x + 17}" y="210" text-anchor="middle" class="chart-label">${escapeHtml(String(row.studentName).slice(0, 9))}</text><text x="${x + 17}" y="${184 - barHeight}" text-anchor="middle" class="chart-value">${row.totalBorrowed}</text>`;
  }).join('');
  return `<div class="chart-scroll"><svg class="analysis-svg" viewBox="0 0 ${width} 235" role="img" aria-label="Books borrowed per student"><line x1="25" y1="190" x2="${width - 20}" y2="190" class="chart-axis"/>${bars}</svg></div>`;
}
function donutPath(cx, cy, outer, inner, start, end) {
  const point = (radius, angle) => [cx + radius * Math.cos(angle), cy + radius * Math.sin(angle)];
  const a = point(outer, start), b = point(outer, end), c = point(inner, end), d = point(inner, start);
  const large = end - start > Math.PI ? 1 : 0;
  return `M ${a[0]} ${a[1]} A ${outer} ${outer} 0 ${large} 1 ${b[0]} ${b[1]} L ${c[0]} ${c[1]} A ${inner} ${inner} 0 ${large} 0 ${d[0]} ${d[1]} Z`;
}
function departmentDonutChart(rows) {
  if (!rows.length) return chartEmpty();
  const colors = ['#315b91','#57a58b','#e2a947','#8c78bd','#d47768','#4e9fbd','#8897aa'];
  const total = rows.reduce((sum, row) => sum + row.totalBorrowed, 0);
  if (!total) return chartEmpty();
  let angle = -Math.PI / 2;
  const slices = rows.map((row, i) => {
    const next = angle + row.totalBorrowed / total * Math.PI * 2;
    const path = donutPath(120, 120, 96, 57, angle, next);
    angle = next;
    return `<path d="${path}" fill="${colors[i % colors.length]}"><title>${escapeHtml(row.department)}: ${row.totalBorrowed}</title></path>`;
  }).join('');
  const legend = rows.map((row, i) => `<li><span style="--legend-color:${colors[i % colors.length]}"></span><b>${escapeHtml(row.department)}</b><small>${row.totalBorrowed} borrows</small></li>`).join('');
  return `<div class="donut-layout"><svg class="donut-svg" viewBox="0 0 240 240" role="img" aria-label="Borrowing share by department">${slices}<text x="120" y="115" text-anchor="middle" class="donut-total">${total}</text><text x="120" y="137" text-anchor="middle" class="donut-caption">borrows</text></svg><ul class="donut-legend">${legend}</ul></div>`;
}
function monthlyLineChart(rows) {
  if (!rows.length || rows.every(row => !row.totalBorrowed)) return chartEmpty();
  const width = 720, height = 260, left = 38, right = 18, top = 24, bottom = 52;
  const max = Math.max(1, ...rows.map(row => row.totalBorrowed));
  const x = i => left + i * (width - left - right) / Math.max(1, rows.length - 1);
  const y = value => height - bottom - value / max * (height - top - bottom);
  const points = rows.map((row, i) => `${x(i)},${y(row.totalBorrowed)}`).join(' ');
  const markers = rows.map((row, i) => `<circle cx="${x(i)}" cy="${y(row.totalBorrowed)}" r="4" class="chart-point"><title>${escapeHtml(row.label)}: ${row.totalBorrowed}</title></circle><text x="${x(i)}" y="${height - 24}" text-anchor="middle" class="chart-label">${escapeHtml(row.label)}</text>`).join('');
  const grid = [0, .25, .5, .75, 1].map(fraction => {
    const val = Math.round(max * fraction);
    const yy = y(val);
    return `<line x1="${left}" y1="${yy}" x2="${width - right}" y2="${yy}" class="chart-grid"/><text x="${left - 8}" y="${yy + 4}" text-anchor="end" class="chart-label">${val}</text>`;
  }).join('');
  return `<div class="chart-scroll"><svg class="analysis-svg line-svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="Monthly borrowing trend">${grid}<polyline points="${points}" fill="none" stroke="#315b91" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>${markers}</svg></div>`;
}
function categoryBarChart(rows) {
  if (!rows.length) return chartEmpty();
  const max = Math.max(1, ...rows.map(row => row.totalBorrowed));
  return `<div class="category-bars">${rows.map(row => `<div class="category-row"><span title="${escapeHtml(row.category)}">${escapeHtml(row.category)}</span><div class="category-track"><i style="width:${row.totalBorrowed / max * 100}%"></i></div><b>${row.totalBorrowed}</b></div>`).join('')}</div>`;
}
function readersLeaderboard(rows) {
  if (!rows.length) return chartEmpty();
  return `<ol class="reader-list">${rows.map((row, i) => `<li><span class="reader-rank">${i + 1}</span><div class="reader-name"><b>${escapeHtml(row.studentName)}</b><small>${escapeHtml(row.department)}</small></div><strong>${row.totalBorrowed}<small> borrows</small></strong></li>`).join('')}</ol>`;
}
async function renderAnalytics(filters = {}) {
  content().innerHTML = pageHead('Student Reading Analysis', 'Explore borrowing activity across students, departments and book categories.') +
    '<div class="loading">Loading reading analysis…</div>';
  const params = new URLSearchParams();
  if (filters.startDate) params.set('startDate', filters.startDate);
  if (filters.endDate) params.set('endDate', filters.endDate);
  if (filters.department) params.set('department', filters.department);
  const query = params.toString() ? `?${params}` : '';
  try {
    const [students, summary, studentCounts, departments, months, categories, readers] = await Promise.all([
      request('/students'), request(`/analytics/summary${query}`),
      request(`/analytics/books-per-student${query}`), request(`/analytics/department-reading${query}`),
      request(`/analytics/monthly-trend${query}`), request(`/analytics/top-categories${query}`),
      request(`/analytics/top-readers${query}`)
    ]);
    const departmentOptions = [...new Set(students.map(student => student.department).filter(Boolean))]
      .sort((a, b) => a.localeCompare(b))
      .map(department => `<option value="${escapeHtml(department)}" ${filters.department === department ? 'selected' : ''}>${escapeHtml(department)}</option>`).join('');
    content().innerHTML = pageHead('Student Reading Analysis', 'Explore borrowing activity across students, departments and book categories.') +
      `<form id="analysisFilters" class="analysis-filters"><label>Department<select name="department"><option value="">All departments</option>${departmentOptions}</select></label><label>From<input type="date" name="startDate" value="${escapeHtml(filters.startDate)}"></label><label>To<input type="date" name="endDate" value="${escapeHtml(filters.endDate)}"></label><button class="primary">Apply filters</button><button type="reset" class="secondary" id="clearAnalysisFilters">Clear</button></form>
      <section class="stats analysis-stats"><article><span>Total students</span><strong>${summary.totalStudents}</strong><small>In selected department</small></article><article><span>Total books borrowed</span><strong>${summary.totalBorrowed}</strong><small>In selected date range</small></article><article><span>Active readers this month</span><strong>${summary.activeReadersThisMonth}</strong><small>Students with a borrow this month</small></article><article><span>Average borrows per student</span><strong>${Number(summary.averageBooksPerStudent).toFixed(2)}</strong><small>For selected date range</small></article></section>
      <div class="analysis-grid"><section class="panel analysis-panel"><h2>Books borrowed per student</h2>${studentBarChart(studentCounts)}</section>
      <section class="panel analysis-panel"><h2>Reading share by department</h2>${departmentDonutChart(departments)}</section>
      <section class="panel analysis-panel analysis-wide"><h2>Monthly borrowing trend</h2><p class="chart-subtitle">Last 12 months</p>${monthlyLineChart(months)}</section>
      <section class="panel analysis-panel"><h2>Top book categories</h2>${categoryBarChart(categories)}</section>
      <section class="panel analysis-panel"><h2>Top 5 readers</h2>${readersLeaderboard(readers)}</section></div>`;
    document.querySelector('#analysisFilters').onsubmit = event => {
      event.preventDefault();
      const formData = new FormData(event.currentTarget);
      renderAnalytics({ department: formData.get('department'), startDate: formData.get('startDate'), endDate: formData.get('endDate') });
    };
    document.querySelector('#clearAnalysisFilters').onclick = event => {
      event.preventDefault();
      renderAnalytics();
    };
  } catch (error) {
    content().innerHTML = pageHead('Student Reading Analysis', 'Explore borrowing activity across students, departments and book categories.') +
      `<div class="error-page"><h2>Could not load reading analysis</h2><p>${escapeHtml(error.message)}</p><button class="primary" id="retryAnalysis">Try again</button></div>`;
    document.querySelector('#retryAnalysis').onclick = () => renderAnalytics(filters);
  }
}

async function renderPage() {
  try {
    if (user.role === 'ADMIN' && active === 'analytics') {
      await renderAnalytics();
    } else if (user.role === 'ADMIN' && active === 'dashboard') {
      const [s, books, loans] = await Promise.all([request('/reports/summary'), request('/books'), request('/loans')]);
      content().innerHTML = pageHead('Dashboard', 'Here is what is happening in your library today.') + `
        <section class="stats"><article><span>Total book copies</span><strong>${s.totalBooks}</strong><small>Across all titles</small></article><article><span>Available copies</span><strong>${s.availableBooks}</strong><small>Ready to borrow</small></article><article><span>Currently issued</span><strong>${s.issuedBooks}</strong><small>Not yet returned</small></article><article><span>Overdue books</span><strong>${s.overdueBooks}</strong><small>Need attention</small></article></section>
        <section class="panel"><div class="panel-title"><div><h2>Recent borrowing activity</h2><p>Latest library transactions</p></div><button class="secondary" id="viewLoans">View all</button></div>${table(['Book','Student','Issue date','Due date','Status'],loans.slice(-5).reverse().map(x=>`<tr><td><b>${x.bookTitle}</b></td><td>${x.studentName}</td><td>${x.issueDate}</td><td>${x.dueDate}</td><td><span class="pill ${x.status.toLowerCase()}">${x.status}</span></td></tr>`).join(''))}</section>
        <section class="panel"><h2>Book availability</h2>${table(['Title','Author','Category','Available'],books.map(b=>`<tr><td>${b.title}</td><td>${b.author}</td><td>${b.category||'-'}</td><td>${b.availableQuantity} / ${b.totalQuantity}</td></tr>`).join(''))}</section>`;
      document.querySelector('#viewLoans').onclick = () => { active = 'loans'; renderApp(); };
    } else if (user.role === 'ADMIN' && active === 'books') {
      const books = await request('/books');
      content().innerHTML = pageHead('Book collection','Maintain the books available in your college library.') + `<section class="panel"><h2>Add a new book</h2><form id="bookForm" class="form-grid"><input name="title" placeholder="Book title" required><input name="author" placeholder="Author" required><input name="category" placeholder="Category"><input name="totalQuantity" type="number" min="1" placeholder="Quantity" required><button class="primary">Add book</button></form></section><section class="panel"><h2>All books (${books.length})</h2>${table(['ID','Title','Author','Category','Available copies'],books.map(b=>`<tr><td>${b.id}</td><td><b>${b.title}</b></td><td>${b.author}</td><td>${b.category||'-'}</td><td>${b.availableQuantity} / ${b.totalQuantity}</td></tr>`).join(''))}</section>`;
      document.querySelector('#bookForm').onsubmit = async e => { e.preventDefault(); const f=new FormData(e.target); try { await request('/books',{method:'POST',body:JSON.stringify({title:f.get('title'),author:f.get('author'),category:f.get('category'),totalQuantity:Number(f.get('totalQuantity'))})}); renderPage(); } catch(err){notify(err.message,true);} };
    } else if (user.role === 'ADMIN' && active === 'students') {
      const students = await request('/students');
      content().innerHTML = pageHead('Student members','Register students and sign in with their assigned Student ID.') + `<section class="panel"><h2>Register a student login</h2><form id="studentForm" class="form-grid"><input name="name" placeholder="Student name" required><input name="email" type="email" placeholder="Email address" required><input name="department" placeholder="Department"><input name="password" type="password" minlength="6" placeholder="Temporary password (min 6 characters)" required><button class="primary">Register student</button></form></section><section class="panel"><h2>Registered students (${students.length})</h2>${table(['Student ID','Name','Email','Department','Action'],students.map(s=>`<tr><td><b>${escapeHtml(s.studentCode || `stu${s.id}`)}</b></td><td>${escapeHtml(s.name)}</td><td>${escapeHtml(s.email)}</td><td>${escapeHtml(s.department||'-')}</td><td><button class="small-btn" data-delete-student="${s.id}">Delete</button></td></tr>`).join(''))}</section>`;
      document.querySelector('#studentForm').onsubmit = async e => { e.preventDefault(); const form=e.target; const button=form.querySelector('button'); const f=new FormData(form); button.disabled=true; try { const student=await request('/students',{method:'POST',body:JSON.stringify({name:f.get('name'),email:f.get('email'),department:f.get('department'),password:f.get('password')})}); await renderPage(); notify(`Student registered. Student ID: ${student.studentCode}`); } catch(err){notify(err.message,true); button.disabled=false;} };
      document.querySelectorAll('[data-delete-student]').forEach(button=>button.onclick=async()=>{if(!confirm('Delete this student? Students with loan history cannot be deleted.'))return;button.disabled=true;try{await request(`/students/${button.dataset.deleteStudent}`,{method:'DELETE'});renderPage();}catch(err){notify(err.message,true);button.disabled=false;}});
    } else if (user.role === 'ADMIN' && active === 'loans') {
      const [books,students,loans] = await Promise.all([request('/books'),request('/students'),request('/loans')]);
      content().innerHTML = pageHead('Issue & return books','Track borrowing, due dates and returns.') + `<section class="panel"><h2>Issue a book</h2><form id="issueForm" class="form-grid"><select name="bookId" required><option value="">Choose a book</option>${books.filter(b=>b.availableQuantity>0).map(b=>`<option value="${b.id}">${b.title} (${b.availableQuantity} available)</option>`).join('')}</select><select name="studentId" required><option value="">Choose a student</option>${students.map(s=>`<option value="${s.id}">${escapeHtml(s.studentCode || `stu${s.id}`)} — ${escapeHtml(s.name)}</option>`).join('')}</select><button class="primary">Issue book</button></form></section><section class="panel"><h2>Borrowing history</h2>${table(['Book','Student','Issued','Due date','Status','Action'],loans.slice().reverse().map(x=>`<tr><td><b>${x.bookTitle}</b></td><td>${x.studentName}</td><td>${x.issueDate}</td><td>${x.dueDate}</td><td><span class="pill ${x.status.toLowerCase()}">${x.status}</span></td><td>${x.returnDate?'<span class="muted">Returned</span>':`<button class="small-btn" data-return="${x.id}">Return</button>`}</td></tr>`).join(''))}</section>`;
      document.querySelector('#issueForm').onsubmit = async e => {e.preventDefault();const f=new FormData(e.target);try{await request('/loans/issue',{method:'POST',body:JSON.stringify({bookId:Number(f.get('bookId')),studentId:Number(f.get('studentId'))})});renderPage();}catch(err){notify(err.message,true);}};
      document.querySelectorAll('[data-return]').forEach(b=>b.onclick=async()=>{try{await request(`/loans/${b.dataset.return}/return`,{method:'POST'});renderPage();}catch(err){notify(err.message,true);}});
    } else {
      const [books, loans] = await Promise.all([request('/books'), request('/student/loans')]);
      content().innerHTML = pageHead(`Welcome, ${user.studentName || user.username}`,'Browse the library collection and check your own borrowing history.') + `<section class="panel"><h2>Available books</h2>${table(['Title','Author','Category','Availability'],books.map(b=>`<tr><td><b>${b.title}</b></td><td>${b.author}</td><td>${b.category||'-'}</td><td>${b.availableQuantity>0?`<span class="pill returned">${b.availableQuantity} available</span>`:'<span class="pill overdue">Currently unavailable</span>'}</td></tr>`).join(''))}</section><section class="panel"><h2>My borrowing history</h2>${table(['Book','Issue date','Due date','Return date','Status'],loans.map(x=>`<tr><td><b>${x.bookTitle}</b></td><td>${x.issueDate}</td><td>${x.dueDate}</td><td>${x.returnDate||'-'}</td><td><span class="pill ${x.status.toLowerCase()}">${x.status}</span></td></tr>`).join(''))}</section>`;
    }
  } catch (err) {
    if (err.message.toLowerCase().includes('log in') || err.message.includes('401')) { user=null; loginScreen('Your session ended. Please log in again.'); return; }
    content().innerHTML = `<div class="error-page"><h2>Cannot load library data</h2><p>${err.message}</p><p>Confirm the Java backend is running in Terminal 1.</p><button class="primary" onclick="location.reload()">Try again</button></div>`;
  }
}
async function renderApp() {
  shell(user.role === 'ADMIN' ? 'Dashboard' : 'My Library', '', '<div class="loading">Loading…</div>');
  await renderPage();
}
async function start() {
  try {
    const session = await request('/auth/me');
    user = { ...session, role: session.role };
    if (user.role === 'STUDENT') user.studentName = session.studentName || user.username;
    active = user.role === 'ADMIN' ? 'dashboard' : 'student-home';
    renderApp();
  } catch { loginScreen(); }
}
start();
