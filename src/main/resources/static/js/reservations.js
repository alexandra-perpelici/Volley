const cells = document.querySelectorAll("td.available, td.reserved");

cells.forEach(cell => {
    cell.addEventListener("click", () => {
        if (cell.classList.contains("reserved")) {
            showToast("Ora este deja rezervata.");
            return;
        }

        // Show popup
        document.getElementById("popupSlot").innerText = "Ora: " + cell.innerText;
        document.getElementById("reservationPopup").style.display = "block";

        // Yes button
        document.getElementById("popupYesBtn").onclick = () => {
            const [date, time] = parseSlot(cell.innerText);
            const fieldNumber = window.location.pathname.includes("/1") ? 1 : 2;

            // Fill hidden form values
            document.getElementById("formReservationDate").value = date;
            document.getElementById("formReservationTime").value = time;
            document.getElementById("formFieldNumber").value = fieldNumber;

            // Submit the hidden form
            document.getElementById("reservationForm").submit();
        };

        // No button
        document.getElementById("popupNoBtn").onclick = () => {
            document.getElementById("reservationPopup").style.display = "none";
        };
    });
});

function parseSlot(text) {
    // text format: "dd-MM-yyyy hh:mm - hh:mm"
    const parts = text.split(" ");
    const date = parts[0];
    const time = parts.slice(1).join(" ");
    return [date, time];
}

function showToast(message) {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.style.visibility = "visible";
    setTimeout(() => { toast.style.visibility = "hidden"; }, 2500);
}
