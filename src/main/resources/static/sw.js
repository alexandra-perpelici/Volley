const APP_SHELL_CACHE = "volley-shell-v1";

self.addEventListener("install", event => {
    event.waitUntil(
        caches.open(APP_SHELL_CACHE).then(cache => cache.addAll([
            "/css/site.css",
            "/img/icons/icon-192.png",
            "/img/icons/icon-512.png"
        ]))
    );
    self.skipWaiting();
});

self.addEventListener("activate", event => {
    event.waitUntil(self.clients.claim());
});

self.addEventListener("fetch", event => {
    if (event.request.method !== "GET") {
        return;
    }

    event.respondWith(
        fetch(event.request).catch(() => caches.match(event.request))
    );
});

self.addEventListener("push", event => {
    let payload = {};

    if (event.data) {
        try {
            payload = event.data.json();
        } catch (error) {
            payload = { body: event.data.text() };
        }
    }

    const title = payload.title || "Volley Reservations";
    const options = {
        body: payload.body || "New reservation activity.",
        icon: "/img/icons/icon-192.png",
        badge: "/img/icons/icon-192.png",
        tag: payload.tag || "volley-reservation",
        data: {
            url: payload.url || "/admin"
        }
    };

    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener("notificationclick", event => {
    event.notification.close();

    const targetUrl = new URL(event.notification.data?.url || "/admin", self.location.origin).href;

    event.waitUntil(
        clients.matchAll({ type: "window", includeUncontrolled: true }).then(clientList => {
            for (const client of clientList) {
                if (client.url === targetUrl && "focus" in client) {
                    return client.focus();
                }
            }

            if (clients.openWindow) {
                return clients.openWindow(targetUrl);
            }

            return undefined;
        })
    );
});
