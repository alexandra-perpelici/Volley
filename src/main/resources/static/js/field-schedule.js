(() => {
    const dayTabs = document.querySelectorAll(".day-tab");
    const dayPanels = document.querySelectorAll(".day-schedule-panel");
    const slotCards = document.querySelectorAll(".slot-card");
    const popup = document.getElementById("reservationPopup");
    const overlay = document.getElementById("popupOverlay");
    const yesButton = document.getElementById("popupYesBtn");
    const noButton = document.getElementById("popupNoBtn");
    const fieldNumber = document.getElementById("cartFieldNumber")?.closest("form")?.querySelector("[name='fieldNumber']")?.value
        || document.querySelector(".slot-card")?.dataset.field
        || "1";

    dayTabs.forEach(tab => {
        tab.addEventListener("click", () => {
            const dayIndex = tab.dataset.dayIndex;

            dayTabs.forEach(item => item.classList.toggle("active", item === tab));
            dayPanels.forEach(panel => {
                panel.classList.toggle("active", panel.dataset.dayIndex === dayIndex);
            });
        });
    });

    slotCards.forEach(card => {
        card.addEventListener("click", () => {
            if (card.classList.contains("reserved")) {
                showToast("This slot is already reserved.");
                return;
            }

            if (card.classList.contains("selected")) {
                showToast("This slot is already in your cart.");
                return;
            }

            openReservationPopup(card);
        });
    });

    if (noButton) {
        noButton.addEventListener("click", hidePopup);
    }

    function openReservationPopup(card) {
        document.getElementById("popupSlot").innerText = `${card.dataset.displayDate} - ${card.dataset.time}`;

        overlay.style.display = "block";
        popup.style.display = "block";
        setTimeout(() => popup.classList.add("show"), 10);

        yesButton.onclick = () => {
            yesButton.disabled = true;
            yesButton.classList.add("is-loading");
            document.getElementById("cartReservationDate").value = card.dataset.date;
            document.getElementById("cartReservationTime").value = card.dataset.time;
            document.getElementById("cartFieldNumber").value = card.dataset.field;
            window.VolleyNavigationCache?.clear();
            document.getElementById("addToCartForm").submit();
        };
    }

    function hidePopup() {
        popup.classList.remove("show");
        setTimeout(() => {
            popup.style.display = "none";
            overlay.style.display = "none";
            yesButton.disabled = false;
            yesButton.classList.remove("is-loading");
        }, 220);
    }

    function showToast(message) {
        const toast = document.getElementById("toast");
        toast.innerText = message;
        toast.style.visibility = "visible";
        setTimeout(() => {
            toast.style.visibility = "hidden";
        }, 2400);
    }

    function updateReservationMatrix() {
        fetch(`/field/${fieldNumber}/matrix`)
            .then(res => res.json())
            .then(matrix => {
                document.querySelectorAll(".slot-card").forEach(card => {
                    if (card.dataset.selected === "true") {
                        setSlotState(card, "selected");
                        return;
                    }

                    setSlotState(card, matrix[card.dataset.key] ? "reserved" : "available");
                });
            })
            .catch(() => {});
    }

    function setSlotState(card, state) {
        card.classList.remove("available", "reserved", "selected");
        card.classList.add(state);
        const status = card.querySelector(".slot-status");
        if (status) {
            status.innerText = state.charAt(0).toUpperCase() + state.slice(1);
        }
    }

    setInterval(updateReservationMatrix, 30000);
    updateReservationMatrix();
})();
