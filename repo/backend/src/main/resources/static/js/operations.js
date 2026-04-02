(() => {
    const root = document.getElementById("operationsRoot");
    if (!root) {
        return;
    }

    const canViewExceptions = root.dataset.canViewExceptions === "true";
    const canViewTaskWorkspace = root.dataset.canViewTaskWorkspace === "true";
    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
    const writeHeaders = { "Content-Type": "application/json" };
    writeHeaders[csrfHeader] = csrfToken;
    const filterForm = document.getElementById("filterForm");
    const kpiError = document.getElementById("kpiError");
    const csvLink = document.getElementById("csvExport");
    const xlsxLink = document.getElementById("xlsxExport");

    function showKpiSkeleton() {
        document.querySelectorAll(".kpi").forEach((k) => {
            k.classList.add("skeleton");
            const v = k.querySelector(".value");
            if (v) {
                v.textContent = "loading...";
            }
        });
    }

    function toKpiParams() {
        const params = new URLSearchParams();
        const fd = new FormData(filterForm);
        fd.forEach((v, k) => {
            const value = String(v).trim();
            if (value.length > 0) {
                params.set(k, value);
            }
        });
        return params;
    }

    async function loadKpis() {
        kpiError.style.display = "none";
        showKpiSkeleton();
        const params = toKpiParams();
        if (!params.has("from") || !params.has("to")) {
            const to = new Date();
            const from = new Date(Date.now() - (7 * 24 * 60 * 60 * 1000));
            params.set("from", from.toISOString().slice(0, 19));
            params.set("to", to.toISOString().slice(0, 19));
        }
        csvLink.href = "/api/analytics/export.csv?" + params.toString();
        xlsxLink.href = "/api/analytics/export.xlsx?" + params.toString();

        const res = await fetch("/api/analytics/kpis?" + params.toString(), { credentials: "same-origin" });
        const body = await res.json();
        if (!res.ok) {
            kpiError.textContent = body.message || "Failed to load KPI dashboard.";
            kpiError.style.display = "block";
            return;
        }
        document.querySelectorAll(".kpi").forEach((k) => k.classList.remove("skeleton"));
        document.getElementById("gmv").textContent = "$" + Number(body.gmv).toFixed(2);
        document.getElementById("orderVolume").textContent = String(body.orderVolume);
        document.getElementById("conversionRate").textContent = Number(body.conversionRatePercent).toFixed(2) + "%";
        document.getElementById("aov").textContent = "$" + Number(body.averageOrderValue).toFixed(2);
        document.getElementById("repeatRate").textContent = Number(body.repeatPurchaseRatePercent).toFixed(2) + "%";
        document.getElementById("fulfillment").textContent = Number(body.fulfillmentTimelinessPercent).toFixed(2) + "%";

        const cancelRows = document.getElementById("cancelRows");
        cancelRows.innerHTML = "";
        if (!body.cancellationReasons || body.cancellationReasons.length === 0) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 2;
            td.textContent = "No cancellation records for selected filters.";
            tr.appendChild(td);
            cancelRows.appendChild(tr);
            return;
        }
        body.cancellationReasons.forEach((item) => {
            const tr = document.createElement("tr");
            const reason = document.createElement("td");
            reason.textContent = String(item.reason);
            const count = document.createElement("td");
            count.textContent = String(item.count);
            tr.appendChild(reason);
            tr.appendChild(count);
            cancelRows.appendChild(tr);
        });
    }

    async function loadPackages() {
        const res = await fetch("/api/analytics/report-packages", { credentials: "same-origin" });
        if (!res.ok) {
            return;
        }
        const body = await res.json();
        const list = document.getElementById("packages");
        list.innerHTML = "";
        if (!body.length) {
            const li = document.createElement("li");
            li.textContent = "No report packages available.";
            list.appendChild(li);
            return;
        }
        body.forEach((name) => {
            const li = document.createElement("li");
            const a = document.createElement("a");
            a.href = "/api/analytics/report-packages/download?file=" + encodeURIComponent(name);
            a.textContent = name;
            li.appendChild(a);
            list.appendChild(li);
        });
    }

    function showTableSkeleton(targetId, cols, rows) {
        const tbody = document.getElementById(targetId);
        if (!tbody) {
            return;
        }
        tbody.innerHTML = "";
        for (let i = 0; i < rows; i += 1) {
            const tr = document.createElement("tr");
            tr.className = "skeleton-row";
            let cells = "";
            for (let c = 0; c < cols; c += 1) {
                cells += "<td>loading</td>";
            }
            tr.innerHTML = cells;
            tbody.appendChild(tr);
        }
    }

    async function loadPendingExceptions() {
        if (!canViewExceptions) {
            return;
        }
        showTableSkeleton("exceptionRows", 5, 4);
        const res = await fetch("/api/exceptions/pending", { credentials: "same-origin" });
        const body = await res.json();
        const tbody = document.getElementById("exceptionRows");
        tbody.innerHTML = "";
        if (!res.ok || !body.length) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 5;
            td.textContent = "No pending requests.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        body.slice(0, 10).forEach((item) => {
            const tr = document.createElement("tr");
            const key = document.createElement("td");
            key.textContent = item.requestKey;
            const type = document.createElement("td");
            type.textContent = item.requestType;
            const status = document.createElement("td");
            status.textContent = item.status;
            const created = document.createElement("td");
            created.textContent = new Date(item.createdAt).toLocaleString();
            const action = document.createElement("td");
            const approve = document.createElement("button");
            approve.type = "button";
            approve.textContent = "Approve";
            approve.onclick = () => decideException(item.id, true);
            const reject = document.createElement("button");
            reject.type = "button";
            reject.textContent = "Reject";
            reject.style.marginLeft = "8px";
            reject.onclick = () => decideException(item.id, false);
            action.appendChild(approve);
            action.appendChild(reject);
            tr.appendChild(key);
            tr.appendChild(type);
            tr.appendChild(status);
            tr.appendChild(created);
            tr.appendChild(action);
            tbody.appendChild(tr);
        });
    }

    async function decideException(id, approved) {
        const res = await fetch("/api/exceptions/" + id + "/decision", {
            method: "PUT",
            credentials: "same-origin",
            headers: writeHeaders,
            body: JSON.stringify({
                approved: approved,
                comment: approved ? "Approved in unified operations view" : "Rejected in unified operations view"
            })
        });
        const msg = document.getElementById("exceptionFeedback");
        msg.style.display = "block";
        if (!res.ok) {
            msg.className = "alert error";
            msg.textContent = "Unable to apply exception decision.";
            return;
        }
        msg.className = "alert success";
        msg.textContent = approved ? "Exception approved." : "Exception rejected.";
        await loadPendingExceptions();
    }

    async function loadTasks() {
        if (!canViewTaskWorkspace) {
            return;
        }
        showTableSkeleton("taskRows", 4, 4);
        const res = await fetch("/api/tasks/my?page=0&size=10", { credentials: "same-origin" });
        const body = await res.json();
        const tbody = document.getElementById("taskRows");
        tbody.innerHTML = "";
        if (!res.ok || !body.content || !body.content.length) {
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 4;
            td.textContent = "No tasks assigned.";
            tr.appendChild(td);
            tbody.appendChild(tr);
            return;
        }
        body.content.forEach((task) => {
            const tr = document.createElement("tr");
            const title = document.createElement("td");
            title.textContent = task.title;
            const status = document.createElement("td");
            status.textContent = task.status;
            const due = document.createElement("td");
            due.textContent = task.dueAt ? new Date(task.dueAt).toLocaleString() : "-";
            const action = document.createElement("td");
            if (task.status !== "COMPLETED") {
                const complete = document.createElement("button");
                complete.type = "button";
                complete.textContent = "Complete";
                complete.onclick = () => completeTask(task.id);
                action.appendChild(complete);
            } else {
                action.textContent = "-";
            }
            tr.appendChild(title);
            tr.appendChild(status);
            tr.appendChild(due);
            tr.appendChild(action);
            tbody.appendChild(tr);
        });
    }

    async function completeTask(taskId) {
        const res = await fetch("/api/tasks/" + taskId + "/complete", {
            method: "PUT",
            credentials: "same-origin",
            headers: writeHeaders
        });
        const msg = document.getElementById("taskFeedback");
        msg.style.display = "block";
        if (!res.ok) {
            msg.className = "alert error";
            msg.textContent = "Unable to complete task.";
            return;
        }
        msg.className = "alert success";
        msg.textContent = "Task completed.";
        await loadTasks();
    }

    filterForm.addEventListener("submit", (e) => {
        e.preventDefault();
        loadKpis();
    });

    loadKpis();
    loadPackages();
    loadPendingExceptions();
    loadTasks();
})();
