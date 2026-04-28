(() => {
    const dayTabs = document.querySelectorAll(".day-tab");
    const dayPanels = document.querySelectorAll(".day-schedule-panel");
    const slotCards = document.querySelectorAll(".slot-card");
    const addToCartForm = document.getElementById("addToCartForm");
    const fieldNumber = document.querySelector(".slot-card")?.dataset.field || "1";

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
                showToast("Ora este deja ocupata.");
                return;
            }

            if (card.classList.contains("selected")) {
                showToast("Ora este deja in cos.");
                return;
            }

            submitSlot(card);
        });
    });

    function submitSlot(card) {
        if (!addToCartForm || card.classList.contains("is-adding")) {
            return;
        }

        card.classList.add("is-adding");
        card.disabled = true;
        const status = card.querySelector(".slot-status");
        if (status) {
            status.innerText = "Se adauga";
        }

        document.getElementById("cartReservationDate").value = card.dataset.date;
        document.getElementById("cartReservationTime").value = card.dataset.time;
        document.getElementById("cartFieldNumber").value = card.dataset.field;
        window.VolleyNavigationCache?.clear();
        addToCartForm.submit();
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
            status.innerText = stateLabels[state] || state;
        }
    }

    const stateLabels = {
        available: "Libera",
        reserved: "Ocupata",
        selected: "Selectata"
    };

    setInterval(updateReservationMatrix, 30000);
    updateReservationMatrix();
})();
