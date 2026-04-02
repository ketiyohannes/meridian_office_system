(() => {
    const form = document.getElementById("searchForm");
    if (!form) {
        return;
    }

    const resultsBody = document.getElementById("resultsBody");
    const searchError = document.getElementById("searchError");
    const pageInfo = document.getElementById("pageInfo");
    const loadMoreBtn = document.getElementById("loadMoreBtn");
    const trendingPanel = document.getElementById("trendingPanel");
    const historyPanel = document.getElementById("historyPanel");
    const recommendationPanel = document.getElementById("recommendationPanel");
    const clearHistoryBtn = document.getElementById("clearHistoryBtn");
    const keywordInput = document.getElementById("keyword");
    const suggestions = document.getElementById("suggestions");
    const csrfToken = document.querySelector("meta[name='_csrf']").getAttribute("content");
    const csrfHeader = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
    const headers = { "Content-Type": "application/json" };
    headers[csrfHeader] = csrfToken;

    let nextCursor = null;
    let hasMore = false;
    let loadedCount = 0;

    function paramsFromForm(cursor) {
        const fd = new FormData(form);
        const params = new URLSearchParams();
        fd.forEach((v, k) => {
            if (String(v).trim() !== "") {
                params.append(k, String(v).trim());
            }
        });
        params.set("size", "10");
        if (cursor) {
            params.set("cursor", cursor);
        }
        return params;
    }

    function showSkeleton() {
        resultsBody.innerHTML = "";
        for (let i = 0; i < 6; i += 1) {
            const tr = document.createElement("tr");
            tr.className = "skeleton-row";
            tr.innerHTML = "<td>loading</td><td>loading</td><td>loading</td><td>loading</td><td>loading</td><td>loading</td><td>loading</td><td>loading</td>";
            resultsBody.appendChild(tr);
        }
    }

    function renderRows(content, append = false) {
        if (!append) {
            resultsBody.innerHTML = "";
        }
        if (!content || content.length === 0) {
            if (append) {
                return;
            }
            const tr = document.createElement("tr");
            const td = document.createElement("td");
            td.colSpan = 8;
            td.textContent = "No products found.";
            tr.appendChild(td);
            resultsBody.appendChild(tr);
            return;
        }

        content.forEach((item) => {
            const tr = document.createElement("tr");
            const values = [
                item.sku,
                item.name,
                item.category,
                "$" + Number(item.price).toFixed(2),
                item.condition,
                new Date(item.postedAt).toLocaleString(),
                item.zipCode,
                item.distanceMiles == null ? "-" : item.distanceMiles + " mi"
            ];
            values.forEach((value) => {
                const td = document.createElement("td");
                td.textContent = String(value);
                tr.appendChild(td);
            });
            resultsBody.appendChild(tr);
        });
    }

    async function doSearch(append = false) {
        if (!append) {
            showSkeleton();
        }
        searchError.style.display = "none";

        const res = await fetch("/api/discovery/search/lazy?" + paramsFromForm(append ? nextCursor : null).toString(), { credentials: "same-origin" });
        const body = await res.json();
        if (!res.ok) {
            const msg = body.message || (body.details && body.details.join(", ")) || "Search failed.";
            searchError.textContent = msg;
            searchError.style.display = "block";
            resultsBody.innerHTML = "";
            return;
        }

        nextCursor = body.nextCursor || null;
        hasMore = !!body.hasMore;
        loadedCount = append ? loadedCount + body.content.length : body.content.length;
        pageInfo.textContent = `Loaded ${loadedCount} result${loadedCount === 1 ? "" : "s"}`;
        loadMoreBtn.disabled = !hasMore;
        loadMoreBtn.textContent = hasMore ? "Load More" : "All Results Loaded";
        renderRows(body.content, append);
    }

    async function loadTrending() {
        const res = await fetch("/api/discovery/trending?limit=10");
        const items = await res.json();
        trendingPanel.innerHTML = "";
        items.forEach((term) => {
            const el = document.createElement("button");
            el.type = "button";
            el.className = "chip";
            el.textContent = term;
            el.onclick = () => { keywordInput.value = term; doSearch(false); };
            trendingPanel.appendChild(el);
        });
    }

    async function loadRecommendations() {
        const res = await fetch("/api/recommendations?surface=SEARCH&limit=8", { credentials: "same-origin" });
        if (!res.ok) {
            recommendationPanel.innerHTML = "<li>Unable to load recommendations.</li>";
            return;
        }
        const items = await res.json();
        recommendationPanel.innerHTML = "";
        if (!items || items.length === 0) {
            recommendationPanel.innerHTML = "<li>No recommendations yet.</li>";
            return;
        }

        items.forEach((item) => {
            const li = document.createElement("li");
            const btn = document.createElement("button");
            btn.type = "button";
            btn.className = "chip";
            btn.textContent = item.sku + " - " + item.name + (item.longTail ? " (Long-tail)" : "");
            btn.onclick = async () => {
                keywordInput.value = item.name;
                await fetch("/api/recommendations/events", {
                    method: "POST",
                    credentials: "same-origin",
                    headers: headers,
                    body: JSON.stringify({
                        eventType: "VIEW",
                        sku: item.sku,
                        categoryName: item.category
                    })
                });
                doSearch(false);
            };
            li.appendChild(btn);
            recommendationPanel.appendChild(li);
        });
    }

    async function loadHistory() {
        const res = await fetch("/api/discovery/history?limit=15");
        const items = await res.json();
        historyPanel.innerHTML = "";
        items.forEach((term) => {
            const el = document.createElement("button");
            el.type = "button";
            el.className = "chip";
            el.textContent = term;
            el.onclick = () => { keywordInput.value = term; doSearch(false); };
            historyPanel.appendChild(el);
        });
    }

    async function loadSuggestions() {
        const q = keywordInput.value.trim();
        if (q.length < 2) {
            suggestions.innerHTML = "";
            return;
        }
        const res = await fetch("/api/discovery/suggestions?q=" + encodeURIComponent(q));
        const items = await res.json();
        suggestions.innerHTML = "";
        items.forEach((item) => {
            const opt = document.createElement("option");
            opt.value = item.value;
            suggestions.appendChild(opt);
        });
    }

    form.addEventListener("submit", (e) => {
        e.preventDefault();
        const payload = {
            eventType: "SEARCH",
            queryText: document.getElementById("keyword").value || null,
            categoryName: document.getElementById("category").value || null
        };
        fetch("/api/recommendations/events", {
            method: "POST",
            credentials: "same-origin",
            headers: headers,
            body: JSON.stringify(payload)
        });
        doSearch(false);
    });

    loadMoreBtn.addEventListener("click", () => {
        if (hasMore) {
            doSearch(true);
        }
    });

    clearHistoryBtn.addEventListener("click", async () => {
        const csrfOnlyHeaders = {};
        csrfOnlyHeaders[csrfHeader] = csrfToken;
        await fetch("/api/discovery/history", { method: "DELETE", credentials: "same-origin", headers: csrfOnlyHeaders });
        await loadHistory();
    });

    keywordInput.addEventListener("input", () => {
        loadSuggestions();
    });

    loadTrending();
    loadHistory();
    loadRecommendations();
    doSearch(false);
})();
