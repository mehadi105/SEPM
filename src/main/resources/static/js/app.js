const TOKEN_KEY = "ums.token";
const GRADES = ["A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D", "F"];
const ROLE_LABEL = { ADMIN: "Admin", TEACHER: "Teacher", STUDENT: "Student" };
const STATUS_LABEL = { ENROLLED: "Enrolled", COMPLETED: "Completed" };

const state = {
    token: null,
    user: null,
    page: null,
    editing: null,
    courseFilter: null,
};

const cache = {
    departments: [],
    courses: [],
    teachers: [],
    students: [],
    enrollments: [],
    teaching: [],
    users: [],
};

let seq = 0;
let toastTimer = 0;

const content = document.getElementById("content");

document.getElementById("login-form").addEventListener("submit", (event) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    login(String(data.get("username") || ""), String(data.get("password") || ""));
});

document.querySelectorAll("[data-username]").forEach((button) => {
    button.addEventListener("click", () => login(button.dataset.username, button.dataset.password));
});

document.getElementById("logout-button").addEventListener("click", () => {
    clearSession();
    showLogin();
    toast("Signed out");
});

content.addEventListener("click", onClick);
content.addEventListener("submit", onSubmit);
content.addEventListener("change", onChange);
content.addEventListener("input", onInput);
window.addEventListener("hashchange", () => {
    if (state.user) show(currentPage());
});

boot();

async function boot() {
    state.token = localStorage.getItem(TOKEN_KEY);
    if (!state.token) {
        showLogin();
        return;
    }
    try {
        setUser(await api("/api/users/me"));
        showApp();
    } catch (error) {
        clearSession();
        showLogin();
    }
}

async function login(username, password) {
    const errorBox = document.getElementById("login-error");
    errorBox.hidden = true;
    try {
        const session = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({ username: username.trim(), password }),
        });
        state.token = session.token;
        localStorage.setItem(TOKEN_KEY, session.token);
        setUser(session);
        showApp();
    } catch (error) {
        errorBox.hidden = false;
        errorBox.textContent = error.message;
    }
}

function setUser(user) {
    state.user = {
        id: user.id ?? user.userId,
        username: user.username,
        email: user.email || "",
        fullName: user.fullName,
        role: user.role,
    };
}

function showLogin() {
    document.getElementById("boot").hidden = true;
    document.getElementById("app-view").hidden = true;
    document.getElementById("login-view").hidden = false;
    document.title = "Sign in · University Management System";
}

function showApp() {
    document.getElementById("boot").hidden = true;
    document.getElementById("login-view").hidden = true;
    document.getElementById("app-view").hidden = false;
    document.getElementById("who-name").textContent = state.user.fullName;
    const role = document.getElementById("who-role");
    role.textContent = ROLE_LABEL[state.user.role] || state.user.role;
    role.className = `badge ${state.user.role.toLowerCase()}`;
    renderNav();
    const page = currentPage();
    if (location.hash !== `#/${page}`) {
        location.hash = `#/${page}`;
        return;
    }
    show(page);
}

function clearSession() {
    state.token = null;
    state.user = null;
    state.page = null;
    state.editing = null;
    state.courseFilter = null;
    localStorage.removeItem(TOKEN_KEY);
    if (location.hash) history.replaceState(null, "", location.pathname + location.search);
}

function currentPage() {
    const name = location.hash.replace(/^#\/?/, "");
    const allowed = new Set(["dashboard", "departments", "courses", "enrollments", "users"]);
    if (!allowed.has(name)) return "dashboard";
    if (name === "users" && state.user.role !== "ADMIN") return "dashboard";
    return name;
}

function renderNav() {
    const study = state.user.role === "STUDENT" ? "My studies" : state.user.role === "TEACHER" ? "Gradebook" : "Enrollments";
    const items = [
        ["dashboard", "Dashboard"],
        ["departments", "Departments"],
        ["courses", "Courses"],
        ["enrollments", study],
    ];
    if (state.user.role === "ADMIN") items.push(["users", "People"]);
    document.getElementById("nav").innerHTML = items
        .map(([page, label]) => `<a href="#/${page}" data-nav="${page}">${label}</a>`)
        .join("");
}

function pageMeta(page) {
    const study = state.user.role === "STUDENT" ? "My studies" : state.user.role === "TEACHER" ? "Gradebook" : "Enrollments";
    return {
        dashboard: ["Overview", "Dashboard"],
        departments: ["Catalog", "Departments"],
        courses: ["Catalog", "Courses"],
        enrollments: ["Records", study],
        users: ["Access", "People"],
    }[page];
}

async function show(page) {
    if (!state.user) return;
    if (state.page !== page) state.editing = null;
    state.page = page;
    const token = ++seq;
    document.querySelectorAll("[data-nav]").forEach((link) => {
        if (link.dataset.nav === page) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
    });
    const [kicker, title] = pageMeta(page);
    document.getElementById("page-kicker").textContent = kicker;
    document.getElementById("page-title").textContent = title;
    document.title = `${title} · University Management System`;
    content.innerHTML = `<p class="muted">Loading…</p>`;
    try {
        const html = await renderPage(page);
        if (token !== seq) return;
        content.innerHTML = html;
    } catch (error) {
        if (token !== seq) return;
        if (error.unauthorized) {
            toast(error.message);
            return;
        }
        content.innerHTML = `<div class="banner" role="alert">${esc(error.message)}</div>`;
    }
}

function renderPage(page) {
    if (page === "dashboard") return renderDashboard();
    if (page === "departments") return renderDepartments();
    if (page === "courses") return renderCourses();
    if (page === "enrollments") return renderEnrollments();
    if (page === "users") return renderUsers();
    return renderDashboard();
}

async function renderDashboard() {
    const role = state.user.role;
    let users = [];
    let departments = [];
    let courses = [];
    let enrollments = [];
    let teaching = [];
    if (role === "ADMIN") {
        [users, departments, courses, enrollments] = await Promise.all([
            api("/api/users"),
            api("/api/departments"),
            api("/api/courses"),
            api("/api/enrollments"),
        ]);
    } else if (role === "TEACHER") {
        [courses, teaching, enrollments] = await Promise.all([
            api("/api/courses"),
            api("/api/courses/teaching"),
            api("/api/enrollments"),
        ]);
    } else {
        [courses, enrollments] = await Promise.all([api("/api/courses"), api("/api/enrollments")]);
    }

    const stats = role === "ADMIN"
        ? [["People", users.length], ["Departments", departments.length], ["Courses", courses.length], ["Enrollments", enrollments.length]]
        : role === "TEACHER"
            ? [["My courses", teaching.length], ["Students", enrollments.length], ["Graded", enrollments.filter((item) => item.grade).length], ["Open", enrollments.filter((item) => !item.grade).length]]
            : [["My courses", enrollments.length], ["In progress", enrollments.filter((item) => item.status === "ENROLLED").length], ["Completed", enrollments.filter((item) => item.status === "COMPLETED").length], ["Catalog", courses.length]];

    const hint = role === "ADMIN"
        ? "Add a department, create a course, and assign Dr. Sarah Khan or Prof. James Okonkwo. Then enroll a student from Enrollments."
        : role === "TEACHER"
            ? "Open the gradebook and save a grade. Dr. Sarah Khan teaches CS101 and CS201. Prof. James Okonkwo teaches MATH101."
            : "Ayesha Rahman starts enrolled in CS101. Enroll in another course, then open My studies after a teacher records a grade.";

    const rows = enrollments.slice(0, 6).map((item) => `
        <tr>
            ${role === "STUDENT" ? "" : `<td>${esc(item.student.fullName)}</td>`}
            <td>${esc(item.course.code)} · ${esc(item.course.title)}</td>
            <td>${statusBadge(item.status)}</td>
            <td>${esc(item.grade || "—")}</td>
        </tr>`).join("");
    const head = role === "STUDENT"
        ? "<tr><th scope=\"col\">Course</th><th scope=\"col\">Status</th><th scope=\"col\">Grade</th></tr>"
        : "<tr><th scope=\"col\">Student</th><th scope=\"col\">Course</th><th scope=\"col\">Status</th><th scope=\"col\">Grade</th></tr>";

    return `
        <p class="lede">Signed in as ${esc(state.user.fullName)}.</p>
        <section class="stats">${stats.map(([label, value]) => `<article class="stat"><p>${esc(label)}</p><strong>${value}</strong></article>`).join("")}</section>
        <section class="panel hint"><h2>What you can do</h2><p>${esc(hint)}</p></section>
        <section class="panel">
            <h2>Recent enrollments</h2>
            ${rows ? `<div class="table-wrap"><table>${head}<tbody>${rows}</tbody></table></div>` : empty("No enrollments yet.")}
        </section>`;
}

async function renderDepartments() {
    cache.departments = await api("/api/departments");
    const editing = state.editing?.type === "department"
        ? cache.departments.find((item) => item.id === state.editing.id)
        : null;
    const admin = state.user.role === "ADMIN";
    const rows = cache.departments.map((item) => `
        <tr data-filter-row="${esc(`${item.code} ${item.name}`.toLowerCase())}">
            <td>${esc(item.code)}</td>
            <td>${esc(item.name)}</td>
            ${admin ? `<td class="actions">
                <button type="button" class="btn small secondary" data-action="edit-department" data-id="${item.id}">Edit</button>
                <button type="button" class="btn small danger" data-action="delete-department" data-id="${item.id}">Delete</button>
            </td>` : ""}
        </tr>`).join("");
    return `
        ${admin ? departmentForm(editing) : ""}
        <section class="panel">
            <div class="panel-head">
                <h2>All departments</h2>
                <input data-filter="table" type="search" placeholder="Filter" aria-label="Filter departments">
            </div>
            ${rows ? `<div class="table-wrap"><table>
                <thead><tr><th scope="col">Code</th><th scope="col">Name</th>${admin ? "<th scope=\"col\">Actions</th>" : ""}</tr></thead>
                <tbody>${rows}</tbody></table></div>` : empty("No departments yet.")}
        </section>`;
}

function departmentForm(editing) {
    return `
        <form class="panel form-grid" data-form="department">
            <h2>${editing ? "Edit department" : "New department"}</h2>
            ${formError()}
            <div class="grid">
                ${field("Name", `<input name="name" required maxlength="120" value="${esc(editing?.name || "")}">`)}
                ${field("Code", `<input name="code" required minlength="2" maxlength="10" pattern="[A-Za-z0-9]{2,10}" value="${esc(editing?.code || "")}">`)}
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">${editing ? "Save changes" : "Create department"}</button>
                ${editing ? `<button class="btn secondary" type="button" data-action="cancel-edit">Cancel</button>` : ""}
            </div>
        </form>`;
}

async function renderCourses() {
    const role = state.user.role;
    const requests = [api("/api/courses"), api("/api/departments")];
    if (role === "ADMIN") requests.push(api("/api/users?role=TEACHER"));
    if (role === "STUDENT") requests.push(api("/api/enrollments"));
    const results = await Promise.all(requests);
    cache.courses = results[0];
    cache.departments = results[1];
    cache.teachers = role === "ADMIN" ? results[2] : [];
    cache.enrollments = role === "STUDENT" ? results[2] : [];
    const editing = state.editing?.type === "course"
        ? cache.courses.find((item) => item.id === state.editing.id)
        : null;

    if (role === "STUDENT") {
        const cards = cache.courses.map((course) => courseCard(course, cache.enrollments.find((item) => item.course.id === course.id))).join("");
        return `<section class="cards">${cards || empty("No courses are in the catalog.")}</section>`;
    }

    const rows = cache.courses.map((course) => `
        <tr data-filter-row="${esc(`${course.code} ${course.title} ${course.department.name} ${teacherName(course)}`.toLowerCase())}">
            <td>${esc(course.code)}</td>
            <td>${esc(course.title)}${course.teacher && course.teacher.id === state.user.id ? " <span class=\"badge teacher\">Your course</span>" : ""}</td>
            <td>${course.credits}</td>
            <td>${esc(course.department.code)}</td>
            <td>${esc(teacherName(course))}</td>
            ${role === "ADMIN" ? `<td class="actions">
                <button type="button" class="btn small secondary" data-action="edit-course" data-id="${course.id}">Edit</button>
                <button type="button" class="btn small danger" data-action="delete-course" data-id="${course.id}">Delete</button>
            </td>` : ""}
        </tr>`).join("");

    return `
        ${role === "ADMIN" ? courseForm(editing) : ""}
        <section class="panel">
            <div class="panel-head">
                <h2>${role === "TEACHER" ? "Course catalog" : "All courses"}</h2>
                <input data-filter="table" type="search" placeholder="Filter" aria-label="Filter courses">
            </div>
            ${rows ? `<div class="table-wrap"><table>
                <thead><tr>
                    <th scope="col">Code</th><th scope="col">Title</th><th scope="col">Credits</th><th scope="col">Department</th><th scope="col">Teacher</th>
                    ${role === "ADMIN" ? "<th scope=\"col\">Actions</th>" : ""}
                </tr></thead>
                <tbody>${rows}</tbody></table></div>` : empty("No courses yet.")}
        </section>`;
}

function courseCard(course, enrollment) {
    let action = `<button class="btn" type="button" data-action="enroll" data-id="${course.id}">Enroll</button>`;
    if (enrollment?.status === "COMPLETED" || enrollment?.grade) action = `<span class="badge completed">Completed · ${esc(enrollment.grade || "")}</span>`;
    else if (enrollment) action = `<span class="badge enrolled">Enrolled</span>`;
    return `
        <article class="card">
            <p class="code">${esc(course.code)}</p>
            <h3>${esc(course.title)}</h3>
            <p class="meta">${esc(course.department.name)} · ${course.credits} credits · ${esc(teacherName(course))}</p>
            <p>${esc(course.description || "No description.")}</p>
            <div>${action}</div>
        </article>`;
}

function courseForm(editing) {
    const departmentOptions = cache.departments.map((item) =>
        `<option value="${item.id}"${editing?.department?.id === item.id ? " selected" : ""}>${esc(item.code)} — ${esc(item.name)}</option>`).join("");
    const teacherOptions = [`<option value="">Unassigned</option>`].concat(cache.teachers.map((item) =>
        `<option value="${item.id}"${editing?.teacher?.id === item.id ? " selected" : ""}>${esc(item.fullName)}</option>`)).join("");
    return `
        <form class="panel form-grid" data-form="course">
            <h2>${editing ? "Edit course" : "New course"}</h2>
            ${formError()}
            <div class="grid">
                ${field("Code", `<input name="code" required minlength="2" maxlength="12" pattern="[A-Za-z0-9]{2,12}" value="${esc(editing?.code || "")}">`)}
                ${field("Title", `<input name="title" required maxlength="160" value="${esc(editing?.title || "")}">`)}
                ${field("Credits", `<input name="credits" type="number" min="1" max="12" required value="${editing?.credits || 3}">`)}
                ${field("Department", `<select name="departmentId" required>${departmentOptions}</select>`)}
                ${field("Teacher", `<select name="teacherId">${teacherOptions}</select>`)}
                <label class="field wide"><span>Description</span><textarea name="description" maxlength="1000">${esc(editing?.description || "")}</textarea></label>
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">${editing ? "Save changes" : "Create course"}</button>
                ${editing ? `<button class="btn secondary" type="button" data-action="cancel-edit">Cancel</button>` : ""}
            </div>
        </form>`;
}

async function renderEnrollments() {
    if (state.user.role === "STUDENT") return renderStudies();
    if (state.user.role === "TEACHER") return renderGradebook();
    return renderAdminEnrollments();
}

async function renderStudies() {
    cache.enrollments = await api("/api/enrollments");
    const rows = cache.enrollments.map((item) => `
        <tr>
            <td>${esc(item.course.code)}</td>
            <td>${esc(item.course.title)}</td>
            <td>${esc(teacherName(item.course))}</td>
            <td>${statusBadge(item.status)}</td>
            <td>${esc(item.grade || "—")}</td>
            <td>${item.status === "ENROLLED" && !item.grade ? `<button type="button" class="btn small danger" data-action="drop" data-id="${item.id}">Drop</button>` : ""}</td>
        </tr>`).join("");
    return `
        <section class="panel">
            <h2>My studies</h2>
            ${rows ? `<div class="table-wrap"><table>
                <thead><tr><th scope="col">Code</th><th scope="col">Course</th><th scope="col">Teacher</th><th scope="col">Status</th><th scope="col">Grade</th><th scope="col"></th></tr></thead>
                <tbody>${rows}</tbody></table></div>` : empty("You are not enrolled in a course yet. Open Courses to join one.")}
        </section>`;
}

async function renderGradebook() {
    const [teaching, enrollments] = await Promise.all([api("/api/courses/teaching"), api("/api/enrollments")]);
    cache.teaching = teaching;
    cache.enrollments = enrollments;
    if (state.courseFilter == null) state.courseFilter = teaching[0] ? String(teaching[0].id) : "";
    if (!teaching.length) return `<section class="panel">${empty("You are not assigned to a course yet.")}</section>`;
    const options = teaching.map((course) =>
        `<option value="${course.id}"${String(course.id) === String(state.courseFilter) ? " selected" : ""}>${esc(course.code)} — ${esc(course.title)}</option>`).join("");
    const visible = enrollments.filter((item) => String(item.course.id) === String(state.courseFilter));
    return `
        <section class="panel">
            <div class="panel-head">
                <h2>Gradebook</h2>
                <label class="field"><span>Course</span><select id="gradebook-course">${options}</select></label>
            </div>
            ${visible.length ? gradeTable(visible, false) : empty("No students are enrolled in this course.")}
        </section>`;
}

async function renderAdminEnrollments() {
    const [students, courses, enrollments] = await Promise.all([
        api("/api/users?role=STUDENT"),
        api("/api/courses"),
        api("/api/enrollments"),
    ]);
    cache.students = students;
    cache.courses = courses;
    cache.enrollments = enrollments;
    const studentOptions = students.map((item) => `<option value="${item.id}">${esc(item.fullName)} (${esc(item.username)})</option>`).join("");
    const courseOptions = courses.map((item) => `<option value="${item.id}">${esc(item.code)} — ${esc(item.title)}</option>`).join("");
    return `
        <form class="panel form-grid" data-form="enroll">
            <h2>Enroll a student</h2>
            ${formError()}
            <div class="grid">
                ${field("Student", `<select name="studentId" required>${studentOptions}</select>`)}
                ${field("Course", `<select name="courseId" required>${courseOptions}</select>`)}
            </div>
            <div class="form-actions"><button class="btn" type="submit">Enroll</button></div>
        </form>
        <section class="panel">
            <h2>All enrollments</h2>
            ${enrollments.length ? gradeTable(enrollments, true) : empty("No enrollments yet.")}
        </section>`;
}

function gradeTable(enrollments, showCourse) {
    const rows = enrollments.map((item) => `
        <tr>
            <td>${esc(item.student.fullName)}</td>
            ${showCourse ? `<td>${esc(item.course.code)}</td>` : ""}
            <td>${statusBadge(item.status)}</td>
            <td><select data-grade-for="${item.id}" aria-label="Grade for ${esc(item.student.fullName)}">${gradeOptions(item.grade)}</select></td>
            <td class="actions">
                <button type="button" class="btn small" data-action="save-grade" data-id="${item.id}">Save</button>
                ${state.user.role === "ADMIN" ? `<button type="button" class="btn small danger" data-action="drop" data-id="${item.id}">Remove</button>` : ""}
            </td>
        </tr>`).join("");
    return `<div class="table-wrap"><table>
        <thead><tr>
            <th scope="col">Student</th>
            ${showCourse ? "<th scope=\"col\">Course</th>" : ""}
            <th scope="col">Status</th><th scope="col">Grade</th><th scope="col"></th>
        </tr></thead>
        <tbody>${rows}</tbody></table></div>`;
}

async function renderUsers() {
    if (state.user.role !== "ADMIN") return empty("Only an admin can manage people.");
    cache.users = await api("/api/users");
    const editing = state.editing?.type === "user"
        ? cache.users.find((item) => item.id === state.editing.id)
        : null;
    const rows = cache.users.map((item) => `
        <tr data-filter-row="${esc(`${item.username} ${item.fullName} ${item.email} ${item.role}`.toLowerCase())}">
            <td>${esc(item.fullName)}</td>
            <td>${esc(item.username)}</td>
            <td>${esc(item.email)}</td>
            <td><span class="badge ${item.role.toLowerCase()}">${esc(ROLE_LABEL[item.role])}</span></td>
            <td>${item.enabled ? "Active" : "Disabled"}</td>
            <td class="actions">
                <button type="button" class="btn small secondary" data-action="edit-user" data-id="${item.id}">Edit</button>
                <button type="button" class="btn small danger" data-action="delete-user" data-id="${item.id}">Delete</button>
            </td>
        </tr>`).join("");
    return `
        ${userForm(editing)}
        <section class="panel">
            <div class="panel-head">
                <h2>People</h2>
                <input data-filter="table" type="search" placeholder="Filter" aria-label="Filter people">
            </div>
            <div class="table-wrap"><table>
                <thead><tr><th scope="col">Name</th><th scope="col">Username</th><th scope="col">Email</th><th scope="col">Role</th><th scope="col">Status</th><th scope="col">Actions</th></tr></thead>
                <tbody>${rows}</tbody></table></div>
        </section>`;
}

function userForm(editing) {
    const self = editing && editing.id === state.user.id;
    const role = editing?.role || "STUDENT";
    const roleField = self
        ? `<input type="hidden" name="role" value="${esc(editing.role)}"><p class="locked">${esc(ROLE_LABEL[editing.role])}. Your own role stays the same.</p>`
        : `<select name="role" required>${["ADMIN", "TEACHER", "STUDENT"].map((item) => `<option value="${item}"${item === role ? " selected" : ""}>${ROLE_LABEL[item]}</option>`).join("")}</select>`;
    const enabledField = self
        ? `<input type="hidden" name="enabled" value="true"><p class="locked">Active</p>`
        : `<select name="enabled">
            <option value="true"${editing && editing.enabled === false ? "" : " selected"}>Active</option>
            <option value="false"${editing && editing.enabled === false ? " selected" : ""}>Disabled</option>
           </select>`;
    return `
        <form class="panel form-grid" data-form="user">
            <h2>${editing ? "Edit person" : "New person"}</h2>
            <p class="muted">Passwords are stored hashed. Leave the password blank when editing to keep the current one.</p>
            ${formError()}
            <div class="grid">
                ${field("Full name", `<input name="fullName" required maxlength="120" value="${esc(editing?.fullName || "")}">`)}
                ${field("Username", editing ? `<p class="locked">${esc(editing.username)}</p>` : `<input name="username" required minlength="3" maxlength="50" pattern="[A-Za-z0-9._\\-]{3,50}" autocomplete="off">`)}
                ${field("Email", `<input name="email" type="email" required maxlength="160" value="${esc(editing?.email || "")}">`)}
                ${field("Password", `<input name="password" type="password" ${editing ? "" : "required"} minlength="6" maxlength="100" autocomplete="new-password" placeholder="${editing ? "Leave blank to keep the current password" : ""}">`)}
                ${field("Role", roleField)}
                ${field("Account", enabledField)}
            </div>
            <div class="form-actions">
                <button class="btn" type="submit">${editing ? "Save changes" : "Create person"}</button>
                ${editing ? `<button class="btn secondary" type="button" data-action="cancel-edit">Cancel</button>` : ""}
            </div>
        </form>`;
}

async function onClick(event) {
    const button = event.target.closest("[data-action]");
    if (!button) return;
    const action = button.dataset.action;
    const id = Number(button.dataset.id);
    try {
        if (action === "cancel-edit") {
            state.editing = null;
            await show(state.page);
            return;
        }
        if (action === "edit-department" || action === "edit-course" || action === "edit-user") {
            state.editing = { type: action.replace("edit-", ""), id };
            await show(state.page);
            return;
        }
        if (action === "delete-department" || action === "delete-course" || action === "delete-user") {
            const label = action === "delete-department" ? "department" : action === "delete-course" ? "course and its enrollments" : "person";
            if (!window.confirm(`Delete this ${label}?`)) return;
            const path = action === "delete-department" ? "departments" : action === "delete-course" ? "courses" : "users";
            await api(`/api/${path}/${id}`, { method: "DELETE" });
            toast("Deleted");
            state.editing = null;
            await show(state.page);
            return;
        }
        if (action === "enroll") {
            await api("/api/enrollments", { method: "POST", body: JSON.stringify({ courseId: id }) });
            toast("Enrolled");
            await show(state.page);
            return;
        }
        if (action === "drop") {
            if (!window.confirm("Remove this enrollment?")) return;
            await api(`/api/enrollments/${id}`, { method: "DELETE" });
            toast("Enrollment removed");
            await show(state.page);
            return;
        }
        if (action === "save-grade") {
            const select = content.querySelector(`[data-grade-for="${id}"]`);
            await api(`/api/enrollments/${id}/grade`, {
                method: "PATCH",
                body: JSON.stringify({ grade: select.value }),
            });
            toast("Grade saved");
            await show(state.page);
        }
    } catch (error) {
        handleError(error);
    }
}

async function onSubmit(event) {
    const form = event.target.closest("form[data-form]");
    if (!form) return;
    event.preventDefault();
    const button = form.querySelector("[type=submit]");
    button.disabled = true;
    const data = Object.fromEntries(new FormData(form).entries());
    try {
        if (form.dataset.form === "department") {
            const body = { name: data.name, code: data.code };
            if (state.editing?.type === "department") {
                await api(`/api/departments/${state.editing.id}`, { method: "PUT", body: JSON.stringify(body) });
                toast("Department updated");
            } else {
                await api("/api/departments", { method: "POST", body: JSON.stringify(body) });
                toast("Department created");
            }
        } else if (form.dataset.form === "course") {
            const body = {
                code: data.code,
                title: data.title,
                description: data.description || null,
                credits: Number(data.credits),
                departmentId: Number(data.departmentId),
                teacherId: data.teacherId ? Number(data.teacherId) : null,
            };
            if (state.editing?.type === "course") {
                await api(`/api/courses/${state.editing.id}`, { method: "PUT", body: JSON.stringify(body) });
                toast("Course updated");
            } else {
                await api("/api/courses", { method: "POST", body: JSON.stringify(body) });
                toast("Course created");
            }
        } else if (form.dataset.form === "user") {
            if (state.editing?.type === "user") {
                const body = {
                    email: data.email,
                    fullName: data.fullName,
                    role: data.role,
                    enabled: data.enabled === "true",
                };
                if (data.password) body.password = data.password;
                await api(`/api/users/${state.editing.id}`, { method: "PUT", body: JSON.stringify(body) });
                toast("Person updated");
            } else {
                await api("/api/users", {
                    method: "POST",
                    body: JSON.stringify({
                        username: data.username,
                        email: data.email,
                        password: data.password,
                        fullName: data.fullName,
                        role: data.role,
                    }),
                });
                toast("Person created");
            }
        } else if (form.dataset.form === "enroll") {
            await api("/api/enrollments", {
                method: "POST",
                body: JSON.stringify({ studentId: Number(data.studentId), courseId: Number(data.courseId) }),
            });
            toast("Student enrolled");
        }
        state.editing = null;
        await show(state.page);
    } catch (error) {
        button.disabled = false;
        handleError(error);
    }
}

function onChange(event) {
    if (event.target.id !== "gradebook-course") return;
    state.courseFilter = event.target.value;
    show(state.page);
}

function onInput(event) {
    if (event.target.dataset.filter !== "table") return;
    const query = event.target.value.trim().toLowerCase();
    content.querySelectorAll("[data-filter-row]").forEach((row) => {
        const text = row.dataset.filterRow || "";
        row.style.display = !query || text.includes(query) ? "" : "none";
    });
}

function handleError(error) {
    toast(error.message || "Something went wrong");
    const box = content.querySelector(".form-error");
    if (box && !error.unauthorized) {
        box.hidden = false;
        box.textContent = error.message;
    }
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body != null && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    if (state.token) headers.set("Authorization", `Bearer ${state.token}`);
    let response;
    try {
        response = await fetch(path, { ...options, headers });
    } catch (error) {
        throw new Error("Cannot reach the server");
    }
    if (response.status === 204) return null;
    const text = await response.text();
    let data = null;
    if (text) {
        try {
            data = JSON.parse(text);
        } catch (error) {
            data = { message: text.slice(0, 180) };
        }
    }
    if (response.status === 401 && !path.startsWith("/api/auth/")) {
        clearSession();
        showLogin();
        const error = new Error(data?.message || "Your session has ended. Sign in again.");
        error.unauthorized = true;
        throw error;
    }
    if (!response.ok) {
        const details = data?.fieldErrors
            ? Object.entries(data.fieldErrors).map(([key, value]) => `${key}: ${value}`).join(", ")
            : "";
        const error = new Error(details ? `${data?.message || "Request failed"} (${details})` : (data?.message || `Request failed (${response.status})`));
        error.status = response.status;
        throw error;
    }
    return data;
}

function toast(message) {
    const el = document.getElementById("toast");
    el.hidden = false;
    el.textContent = message;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        el.hidden = true;
    }, 3200);
}

function field(label, control) {
    return `<label class="field"><span>${label}</span>${control}</label>`;
}

function formError() {
    return `<p class="form-error banner" role="alert" hidden></p>`;
}

function empty(message) {
    return `<p class="empty">${esc(message)}</p>`;
}

function teacherName(course) {
    return course.teacher ? course.teacher.fullName : "Unassigned";
}

function statusBadge(status) {
    const kind = status === "COMPLETED" ? "completed" : "enrolled";
    return `<span class="badge ${kind}">${esc(STATUS_LABEL[status] || status)}</span>`;
}

function gradeOptions(current) {
    const blank = `<option value="">—</option>`;
    return blank + GRADES.map((grade) => `<option value="${grade}"${grade === current ? " selected" : ""}>${grade}</option>`).join("");
}

function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (ch) => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        "\"": "&quot;",
        "'": "&#39;",
    }[ch]));
}
