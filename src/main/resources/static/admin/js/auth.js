// JavaScript para manejo de errores en login UI
document.addEventListener('DOMContentLoaded', () => {
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.has('error')) {
        const alert = document.getElementById('alert');
        alert.textContent = 'Credenciales inválidas. Por favor intente de nuevo.';
        alert.style.display = 'block';
    }
});
