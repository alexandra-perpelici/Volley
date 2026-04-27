(() => {
    const CACHE_PREFIX = "volley:navigation:";
    const CACHE_TTL_MS = 45_000;
    const PREFETCH_DELAY_MS = 350;
    const cachedUrls = new Set();

    window.VolleyNavigationCache = {
        clear: clearNavigationCache,
        prefetch: prefetchPage
    };

    document.addEventListener("DOMContentLoaded", () => {
        attachNavigationCache();
        window.setTimeout(prefetchVisibleNavigation, PREFETCH_DELAY_MS);
    });

    window.addEventListener("popstate", () => {
        const cachedPage = readCachedPage(window.location.href);
        if (cachedPage) {
            renderCachedPage(cachedPage.html, false);
        } else {
            window.location.reload();
        }
    });

    function attachNavigationCache() {
        document.querySelectorAll("a[href]").forEach(link => {
            const url = normalizedEligibleUrl(link.href);
            if (!url) {
                return;
            }

            link.addEventListener("pointerenter", () => prefetchPage(url), { passive: true });
            link.addEventListener("focus", () => prefetchPage(url), { passive: true });
            link.addEventListener("touchstart", () => prefetchPage(url), { passive: true });

            link.addEventListener("click", event => {
                if (event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
                    return;
                }

                const cachedPage = readCachedPage(url);
                if (!cachedPage) {
                    return;
                }

                event.preventDefault();
                window.history.pushState({ volleyNavigation: true }, "", url);
                renderCachedPage(cachedPage.html, false);
            });
        });

        document.querySelectorAll("form[method='post'], form[method='POST']").forEach(form => {
            form.addEventListener("submit", clearNavigationCache);
        });
    }

    function prefetchVisibleNavigation() {
        document.querySelectorAll(".booking-nav a[href], .mobile-bottom-nav a[href], .court-header-actions a[href]").forEach(link => {
            const url = normalizedEligibleUrl(link.href);
            if (url) {
                prefetchPage(url);
            }
        });
    }

    function normalizedEligibleUrl(href) {
        let url;
        try {
            url = new URL(href, window.location.origin);
        } catch (error) {
            return null;
        }

        if (url.origin !== window.location.origin) {
            return null;
        }

        if (!isCacheablePath(url.pathname)) {
            return null;
        }

        url.hash = "";
        return url.href;
    }

    function isCacheablePath(pathname) {
        return pathname === "/home"
            || pathname === "/profile"
            || pathname === "/cart/view"
            || /^\/field\/[12]$/.test(pathname);
    }

    async function prefetchPage(url) {
        if (cachedUrls.has(url) || readCachedPage(url)) {
            return;
        }

        cachedUrls.add(url);
        try {
            const response = await fetch(url, {
                credentials: "same-origin",
                headers: {
                    "X-Volley-Prefetch": "1"
                }
            });

            if (!response.ok) {
                return;
            }

            const html = await response.text();
            if (!isCacheableHtml(html)) {
                return;
            }

            sessionStorage.setItem(cacheKey(url), JSON.stringify({
                cachedAt: Date.now(),
                html
            }));
        } catch (error) {
            cachedUrls.delete(url);
        }
    }

    function readCachedPage(url) {
        try {
            const rawValue = sessionStorage.getItem(cacheKey(url));
            if (!rawValue) {
                return null;
            }

            const cachedPage = JSON.parse(rawValue);
            if (!cachedPage.html || Date.now() - cachedPage.cachedAt > CACHE_TTL_MS) {
                sessionStorage.removeItem(cacheKey(url));
                return null;
            }

            return cachedPage;
        } catch (error) {
            sessionStorage.removeItem(cacheKey(url));
            return null;
        }
    }

    function renderCachedPage(html) {
        document.open();
        document.write(html);
        document.close();
    }

    function isCacheableHtml(html) {
        return html.includes("<body")
            && !html.includes('<form id="loginForm"')
            && !html.includes("Invalid username or password");
    }

    function clearNavigationCache() {
        Object.keys(sessionStorage)
            .filter(key => key.startsWith(CACHE_PREFIX))
            .forEach(key => sessionStorage.removeItem(key));
    }

    function cacheKey(url) {
        return CACHE_PREFIX + url;
    }
})();
