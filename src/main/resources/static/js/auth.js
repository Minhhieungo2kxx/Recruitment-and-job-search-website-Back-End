
    // Hàm xử lý hiển thị/ẩn mật khẩu
    function togglePasswordVisibility() {
        const passwordInput = document.getElementById('password');
        const icon = document.getElementById('togglePasswordIcon');
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        } else {
            passwordInput.type = 'password';
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    }

    // Phần code xử lý đăng nhập giữ nguyên
    document.getElementById('loginForm').addEventListener('submit', async function(e) {
        e.preventDefault();

        const username = document.getElementById('email').value;
        const password = document.getElementById('password').value;
        const errorDiv = document.getElementById('errorMessage');

        // Xóa thông báo lỗi cũ
        errorDiv.style.display = 'none';
        errorDiv.textContent = '';

        try {
            const response = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    username: username,
                    password: password
                })
            });

            const data = await response.json();

            if (data.statusCode == 200) {
                // Lưu token vào localStorage
                localStorage.setItem('accessToken', data.data.access_token);
                localStorage.setItem('userInfo', JSON.stringify(data.data.user));

                // Hiển thị thông báo thành công và chuyển hướng
                errorDiv.classList.remove('alert-danger');
                errorDiv.classList.add('alert-success');
                errorDiv.textContent = 'Đăng nhập thành công! Đang chuyển hướng...';
                errorDiv.style.display = 'block';

                setTimeout(() => {
                    window.location.href = '/chat';
                }, 1000);
            } else {
                errorDiv.classList.remove('alert-success');
                errorDiv.classList.add('alert-danger');
                errorDiv.style.display = 'block';
                errorDiv.textContent = data.error || 'Đăng nhập thất bại';
            }
        } catch (error) {
            errorDiv.classList.remove('alert-success');
            errorDiv.classList.add('alert-danger');
            errorDiv.style.display = 'block';
            errorDiv.textContent = 'Có lỗi xảy ra. Vui lòng thử lại.';
            console.error('Login error:', error);
        }
    });

    // Kiểm tra và hiển thị lỗi OAuth2 từ URL parameters
    window.addEventListener('DOMContentLoaded', function() {
        const urlParams = new URLSearchParams(window.location.search);
        const error = urlParams.get('error');
        const errorDiv = document.getElementById('errorMessage');

        if (error) {
            errorDiv.classList.remove('alert-success');
            errorDiv.classList.add('alert-danger');

            let errorMessage = '';
            switch(error) {
                case 'oauth_failed':
                    errorMessage = 'Đăng nhập Google thất bại. Vui lòng thử lại.';
                    break;
                case 'processing_failed':
                    errorMessage = 'Có lỗi xảy ra khi xử lý thông tin đăng nhập. Vui lòng thử lại.';
                    break;
                case 'missing_token':
                    errorMessage = 'Không thể nhận thông tin đăng nhập từ Google. Vui lòng thử lại.';
                    break;
                default:
                    errorMessage = 'Có lỗi không xác định xảy ra. Vui lòng thử lại.';
            }

            errorDiv.textContent = errorMessage;
            errorDiv.style.display = 'block';

            // Xóa error parameter khỏi URL
            const newUrl = window.location.pathname;
            window.history.replaceState({}, document.title, newUrl);
        }
    });
    // Thêm loading state cho nút Google login
    document.getElementById('googleLoginBtn').addEventListener('click', function(e) {
        this.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang chuyển hướng...';
        this.style.pointerEvents = 'none';
    });




