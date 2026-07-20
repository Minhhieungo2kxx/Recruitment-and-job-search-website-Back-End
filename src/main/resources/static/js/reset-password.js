<!-- JAVASCRIPT: BẬT / TẮT XEM MẬT KHẨU -->

// Hiện / ẩn mật khẩu

function togglePassword(inputId, toggleIconContainer) {
    const passwordInput = document.getElementById(inputId);
    const icon = toggleIconContainer.querySelector('i');

    if (passwordInput.type === "password") {
        passwordInput.type = "text";
        // Đổi sang icon con mắt mở
        icon.classList.remove('bi-eye-slash');
        icon.classList.add('bi-eye');
    } else {
        passwordInput.type = "password";
        // Đổi về icon con mắt gạch chéo (ẩn)
        icon.classList.remove('bi-eye');
        icon.classList.add('bi-eye-slash');
    }
}
