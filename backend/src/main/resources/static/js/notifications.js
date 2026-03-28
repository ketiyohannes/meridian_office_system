(() => {
    const prefForm = document.getElementById("prefForm");
    if (!prefForm) {
        return;
    }

    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
    const headers = { "Content-Type": "application/json" };
    headers[csrfHeader] = csrfToken;

    const subForm = document.getElementById("subForm");
    const noticeBody = document.getElementById("noticeBody");
    const prefError = document.getElementById("prefError");
    const prefSuccess = document.getElementById("prefSuccess");
    const subError = document.getElementById("subError");
    const subSuccess = document.getElementById("subSuccess");
    const noticeError = document.getElementById("noticeError");
    const unreadCount = document.getElementById("unreadCount");
    const markAllReadBtn = document.getElementById("markAllReadBtn");
    const pageInfo = document.getElementById("pageInfo");
    const prevBtn = document.getElementById("prevBtn");
    const nextBtn = document.getElementById("nextBtn");

    let currentPage = 0;
    let lastPage = true;

    function showSkeleton() {
        noticeBody.innerHTML = "";
        for (let i = 0; i < 6; i += 1) {
            const row = document.createElement("tr");
            row.className = "skeleton-row";
            row.innerHTML = "<td>loading</td><td>loading</td><td>loading</td><td>loading</td><td>loading</td>";
            noticeBody.appendChild(row);
        }
    }

    function renderNotices(content) {
        noticeBody.innerHTML = "";
        if (!content || content.length === 0) {
            const row = document.createElement("tr");
            const cell = document.createElement("td");
            cell.colSpan = 5;
            cell.textContent = "No notifications yet.";
            row.appendChild(cell);
            noticeBody.appendChild(row);
            return;
        }

        content.forEach((item) => {
            const row = document.createElement("tr");

            const topic = document.createElement("td");
            topic.textContent = item.topic;

            const message = document.createElement("td");
            const title = document.createElement("strong");
            title.textContent = item.title;
            const body = document.createElement("div");
            body.textContent = item.body;
            message.appendChild(title);
            message.appendChild(body);

            const status = document.createElement("td");
            const badge = document.createElement("span");
            badge.className = "badge " + (item.status === "READ" ? "" : "unread");
            badge.textContent = item.status;
            status.appendChild(badge);

            const sent = document.createElement("td");
            sent.textContent = new Date(item.sentAt).toLocaleString();

            const action = document.createElement("td");
            if (item.status !== "READ") {
                const button = document.createElement("button");
                button.type = "button";
                button.textContent = "Mark Read";
                button.onclick = async () => {
                    await fetch("/api/notifications/" + item.id + "/read", {
                        method: "PUT",
                        credentials: "same-origin",
                        headers: headers
                    });
                    await loadNotifications(currentPage);
                };
                action.appendChild(button);
            }

            row.appendChild(topic);
            row.appendChild(message);
            row.appendChild(status);
            row.appendChild(sent);
            row.appendChild(action);
            noticeBody.appendChild(row);
        });
    }

    async function loadPreferences() {
        const res = await fetch("/api/notifications/preferences", { credentials: "same-origin" });
        const body = await res.json();
        if (!res.ok) {
            prefError.textContent = body.message || "Failed to load preferences.";
            prefError.style.display = "block";
            return;
        }
        document.getElementById("dndStart").value = body.dndStart.slice(0, 5);
        document.getElementById("dndEnd").value = body.dndEnd.slice(0, 5);
        document.getElementById("maxRemindersPerEvent").value = body.maxRemindersPerEvent;
    }

    async function loadSubscriptions() {
        const res = await fetch("/api/notifications/subscriptions", { credentials: "same-origin" });
        const body = await res.json();
        if (!res.ok) {
            subError.textContent = body.message || "Failed to load subscriptions.";
            subError.style.display = "block";
            return;
        }
        const byTopic = new Map(body.map((x) => [x.topic, x.subscribed]));
        document.querySelectorAll(".topic-check").forEach((el) => {
            el.checked = byTopic.get(el.value) === true;
        });
    }

    async function loadNotifications(page = 0) {
        showSkeleton();
        noticeError.style.display = "none";

        const res = await fetch("/api/notifications?page=" + page + "&size=10", { credentials: "same-origin" });
        const body = await res.json();
        if (!res.ok) {
            noticeError.textContent = body.message || "Failed to load notifications.";
            noticeError.style.display = "block";
            noticeBody.innerHTML = "";
            return;
        }

        currentPage = body.page;
        lastPage = body.last;
        pageInfo.textContent = "Page " + (body.page + 1) + " of " + Math.max(body.totalPages, 1);
        prevBtn.disabled = body.first;
        nextBtn.disabled = body.last;
        renderNotices(body.content);
        await loadUnreadCount();
    }

    async function loadUnreadCount() {
        const res = await fetch("/api/notifications/unread-count", { credentials: "same-origin" });
        if (!res.ok) {
            return;
        }
        const count = await res.json();
        unreadCount.textContent = String(count);
    }

    prefForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        prefError.style.display = "none";
        prefSuccess.style.display = "none";

        const payload = {
            dndStart: document.getElementById("dndStart").value,
            dndEnd: document.getElementById("dndEnd").value,
            maxRemindersPerEvent: Number(document.getElementById("maxRemindersPerEvent").value)
        };

        const res = await fetch("/api/notifications/preferences", {
            method: "PUT",
            credentials: "same-origin",
            headers: headers,
            body: JSON.stringify(payload)
        });
        const body = await res.json();
        if (!res.ok) {
            const msg = body.message || (body.details && body.details.join(", ")) || "Failed to update preferences.";
            prefError.textContent = msg;
            prefError.style.display = "block";
            return;
        }
        prefSuccess.textContent = "Preferences updated.";
        prefSuccess.style.display = "block";
    });

    subForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        subError.style.display = "none";
        subSuccess.style.display = "none";

        const payload = Array.from(document.querySelectorAll(".topic-check")).map((el) => ({
            topic: el.value,
            subscribed: el.checked
        }));

        const res = await fetch("/api/notifications/subscriptions", {
            method: "PUT",
            credentials: "same-origin",
            headers: headers,
            body: JSON.stringify(payload)
        });
        const body = await res.json();
        if (!res.ok) {
            const msg = body.message || (body.details && body.details.join(", ")) || "Failed to update subscriptions.";
            subError.textContent = msg;
            subError.style.display = "block";
            return;
        }
        subSuccess.textContent = "Subscriptions updated.";
        subSuccess.style.display = "block";
    });

    prevBtn.addEventListener("click", () => {
        if (currentPage > 0) {
            loadNotifications(currentPage - 1);
        }
    });

    nextBtn.addEventListener("click", () => {
        if (!lastPage) {
            loadNotifications(currentPage + 1);
        }
    });

    markAllReadBtn.addEventListener("click", async () => {
        await fetch("/api/notifications/read-all", {
            method: "PUT",
            credentials: "same-origin",
            headers: headers
        });
        await loadNotifications(0);
    });

    loadPreferences();
    loadSubscriptions();
    loadNotifications(0);
})();
