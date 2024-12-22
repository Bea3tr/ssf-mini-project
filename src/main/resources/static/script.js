var today = new Date().toISOString().split('T')[0];
document.getElementById("date").setAttribute("max", today);

function openForm() {
    document.getElementById("travelForm").style.display = "block";
}

function closeForm() {
    document.getElementById("travelForm").style.display = "none";
}