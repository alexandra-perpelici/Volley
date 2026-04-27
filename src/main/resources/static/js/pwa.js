(() => {
    let deferredInstallPrompt = null;
    let serviceWorkerRegistration = null;
    let vapidPublicKey = "";

    const installButton = document.querySelector("[data-pwa-install]");
    const pushButton = document.querySelector("[data-push-subscribe]");
    const pushTestButton = document.querySelector("[data-push-test]");
    const pushStatus = document.querySelector("[data-push-status]");

    window.addEventListener("beforeinstallprompt", event => {
        event.preventDefault();
        deferredInstallPrompt = event;
        if (installButton) {
            installButton.hidden = false;
        }
    });

    document.addEventListener("DOMContentLoaded", initPwa);

    async function initPwa() {
        if (!("serviceWorker" in navigator)) {
            setPushStatus("Alerts unavailable");
            return;
        }

        serviceWorkerRegistration = await navigator.serviceWorker.register("/sw.js");

        if (installButton) {
            installButton.addEventListener("click", promptInstall);
        }

        if (!pushButton) {
            return;
        }

        if (!("PushManager" in window) || !("Notification" in window)) {
            setPushStatus("Alerts unavailable");
            return;
        }

        const response = await fetch("/api/push/public-key");
        if (!response.ok) {
            setPushStatus("Alerts unavailable");
            return;
        }

        const config = await response.json();
        if (!config.enabled || !config.publicKey) {
            setPushStatus("Alerts need setup");
            return;
        }

        vapidPublicKey = config.publicKey;
        pushButton.hidden = false;
        pushButton.addEventListener("click", subscribeForPush);

        if (pushTestButton) {
            pushTestButton.addEventListener("click", sendTestPush);
        }

        const existingSubscription = await serviceWorkerRegistration.pushManager.getSubscription();
        if (existingSubscription) {
            if (!subscriptionUsesCurrentKey(existingSubscription)) {
                await existingSubscription.unsubscribe();
                if (Notification.permission === "granted") {
                    await subscribeForPush();
                    return;
                }
                setPushStatus("Alerts need refresh");
                return;
            }
            await saveSubscription(existingSubscription);
            markSubscribed();
        } else if (Notification.permission === "denied") {
            setPushStatus("Alerts blocked");
        } else {
            setPushStatus("Alerts off");
        }
    }

    async function promptInstall() {
        if (!deferredInstallPrompt) {
            return;
        }

        deferredInstallPrompt.prompt();
        await deferredInstallPrompt.userChoice;
        deferredInstallPrompt = null;
        installButton.hidden = true;
    }

    async function subscribeForPush() {
        pushButton.disabled = true;
        setPushStatus("Enabling alerts...");

        try {
            const permission = await Notification.requestPermission();
            if (permission !== "granted") {
                setPushStatus("Alerts blocked");
                return;
            }

            const subscription = await serviceWorkerRegistration.pushManager.subscribe({
                userVisibleOnly: true,
                applicationServerKey: urlBase64ToUint8Array(vapidPublicKey)
            });

            await saveSubscription(subscription);
            markSubscribed();
        } catch (error) {
            setPushStatus("Alerts failed");
            pushButton.disabled = false;
        }
    }

    async function saveSubscription(subscription) {
        const response = await fetch("/api/push/subscriptions", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(subscription.toJSON())
        });

        if (!response.ok) {
            throw new Error("Subscription save failed");
        }
    }

    async function sendTestPush() {
        if (!pushTestButton) {
            return;
        }

        pushTestButton.disabled = true;
        setPushStatus("Sending test...");

        try {
            const response = await fetch("/api/push/test", { method: "POST" });
            if (!response.ok) {
                throw new Error("Test failed");
            }
            setPushStatus("Test sent");
        } catch (error) {
            setPushStatus("Test failed");
        } finally {
            pushTestButton.disabled = false;
        }
    }

    function markSubscribed() {
        pushButton.textContent = "Alerts On";
        pushButton.disabled = true;
        setPushStatus("Alerts on");

        if (pushTestButton) {
            pushTestButton.hidden = false;
        }
    }

    function setPushStatus(message) {
        if (pushStatus) {
            pushStatus.textContent = message;
        }
    }

    function subscriptionUsesCurrentKey(subscription) {
        const applicationServerKey = subscription.options?.applicationServerKey;
        if (!applicationServerKey) {
            return false;
        }

        return arrayBufferToBase64Url(applicationServerKey) === normalizeBase64Url(vapidPublicKey);
    }

    function arrayBufferToBase64Url(buffer) {
        const bytes = new Uint8Array(buffer);
        let binary = "";
        bytes.forEach(byte => {
            binary += String.fromCharCode(byte);
        });

        return normalizeBase64Url(window.btoa(binary));
    }

    function normalizeBase64Url(value) {
        return value.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
    }

    function urlBase64ToUint8Array(base64String) {
        const padding = "=".repeat((4 - base64String.length % 4) % 4);
        const base64 = (base64String + padding).replace(/-/g, "+").replace(/_/g, "/");
        const rawData = window.atob(base64);
        return Uint8Array.from([...rawData].map(character => character.charCodeAt(0)));
    }
})();
