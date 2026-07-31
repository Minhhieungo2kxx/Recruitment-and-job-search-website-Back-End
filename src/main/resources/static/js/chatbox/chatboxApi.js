

export const AuthService = {
    accessToken: localStorage.getItem("accessToken"),
    currentUser: null,

    // Tích hợp trực tiếp hàm refreshAccessToken vào đây
    async refreshAccessToken() {
        try {
            const response = await fetch("/api/v1/auth/refresh", {
                method: "POST",
                credentials: "include", // BẮT BUỘC
                headers: { "Content-Type": "application/json" }
            });

            if (!response.ok) return false;

            const result = await response.json();
            if (result.statusCode !== 200 || !result.data?.access_token) {
                return false;
            }

            this.accessToken = result.data.access_token;
            localStorage.setItem("accessToken", this.accessToken);

            if (result.data.user) {
                this.currentUser = result.data.user;
                localStorage.setItem("userInfo", JSON.stringify(this.currentUser));
            }

            console.log("Refresh accessToken thành công");
            return true;

        } catch (err) {
            console.error("Refresh token thất bại", err);
            return false;
        }
    },

    async apiCall(url, options = {}) {
        let accessToken = this.accessToken || localStorage.getItem("accessToken");

        // GLOBAL RATE LIMIT BLOCK (Lưu ý: biến rateLimitUntil cần được định nghĩa toàn cục hoặc trong scope này)
        if (typeof rateLimitUntil !== 'undefined' && rateLimitUntil && Date.now() < rateLimitUntil) {
            return {
                httpStatus: 429,
                statusCode: 429,
                retryAfter: Math.ceil((rateLimitUntil - Date.now()) / 1000)
            };
        }

        const finalOptions = {
            ...options,
            credentials: "include",
            headers: {
                ...(options.headers || {}),
                Authorization: `Bearer ${accessToken}`
            }
        };

        if (options.body && !(options.body instanceof FormData)) {
            finalOptions.headers["Content-Type"] = "application/json";
        }

        let response = await fetch(url, finalOptions);

        // Refresh token khi gặp lỗi 401 hoặc 403
        if (response.status === 401 || response.status === 403) {
            // Gọi trực tiếp hàm refreshAccessToken của chính AuthService
            const refreshed = await this.refreshAccessToken();
            if (!refreshed) throw new Error("Unauthorized");

            finalOptions.headers.Authorization = `Bearer ${this.accessToken}`;
            response = await fetch(url, finalOptions);
        }

        // BẮT RATE LIMIT TẠI ĐÂY
        if (response.status === 429) {
            const retryAfter = Number(response.headers.get("Retry-After")) || 300;

            return {
                httpStatus: 429,
                statusCode: 429,
                retryAfter
            };
        }

        let body = null;
        try {
            body = await response.json();
        } catch (e) {
            body = {
                statusCode: response.status,
                message: response.statusText,
                error: "Unknown Error"
            };
        }

        return {
            httpStatus: response.status,
            statusCode: body.statusCode || response.status,
            message: body.message,
            error: body.error,
            timestamp: body.timestamp,
            data: body.data
        };
    }
};