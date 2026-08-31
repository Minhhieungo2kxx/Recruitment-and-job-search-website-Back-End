
export async function togglePinNotification(notificationId, accessToken) {
    const response = await fetch(
        `/api/v1/notifications/${notificationId}/pin-toggle`,
        {
            method: "PATCH",
            headers: {
                "Authorization": `Bearer ${accessToken}`,
                "Content-Type": "application/json"
            }
        }
    );

    if (!response.ok) {
        throw new Error("Không thể thay đổi trạng thái ghim thông báo");
    }

    return response;
}



export async function toggleReadNotification(notificationId, accessToken) {
    const response = await fetch(
        `/api/v1/notifications/${notificationId}/read-toggle`,
        {
            method: "PATCH",
            headers: {
                "Authorization": `Bearer ${accessToken}`,
                "Content-Type": "application/json"
            }
        }
    );

    if (!response.ok) {
        throw new Error("Không thể thay đổi trạng thái đọc thông báo");
    }

    return response;
}

export async function markNotificationAsRead(notificationId, accessToken) {
    const response = await fetch(
        `/api/v1/notifications/${notificationId}/read`,
        {
            method: "PATCH",
            headers: {
                "Authorization": `Bearer ${accessToken}`,
                "Content-Type": "application/json"
            }
        }
    );

    if (!response.ok) {
        throw new Error(
            "Không thể đánh dấu thông báo đã đọc"
        );
    }

    return response;
}

// notificationApi.js

export async function fetchNotificationsApi(page, pageSize, accessToken) {
    try {
        const response = await fetch(
            `/api/v1/notifications?page=${page}&pageSize=${pageSize}`,
            {
                headers: {
                    Authorization: `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Lỗi gọi API: ${response.status}`);
        }

        const json = await response.json();
        return json;
    } catch (error) {
        console.error("Không thể lấy dữ liệu thông báo:", error);
        throw error;
    }
}


export async function fetchUnreadCountApi(accessToken) {
    try {
        const response = await fetch(
            "/api/v1/notifications/unread-count",
            {
                headers: {
                    Authorization: `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Lỗi gọi API unread-count: ${response.status}`);
        }

        const json = await response.json();
        return json;
    } catch (error) {
        console.error("Không thể lấy số lượng thông báo chưa đọc:", error);
        throw error;
    }
}

// notificationApi.js

export async function markAllAsReadApi(accessToken) {
    try {
        const response = await fetch(
            "/api/v1/notifications/read-all",
            {
                method: "PATCH",
                headers: {
                    Authorization: `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Lỗi cập nhật trạng thái đã đọc: ${response.status}`);
        }
        // Trả về dữ liệu nếu API có response, hoặc trả về true nếu thành công
        return response.status !== 204 ? await response.json() : true;
    } catch (error) {
        console.error("Không thể đánh dấu tất cả là đã đọc:", error);
        throw error;
    }
}

// notificationApi.js

export async function clearAllNotificationsApi(accessToken) {
    try {
        const response = await fetch(
            "/api/v1/notifications/read",
            {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Lỗi xóa thông báo: ${response.status}`);
        }

        return response.status !== 204 ? await response.json() : true;
    } catch (error) {
        console.error("Không thể xóa thông báo đã đọc:", error);
        throw error;
    }
}

// notificationApi.js

export async function deleteNotificationByIdApi(id, accessToken) {
    try {
        const response = await fetch(
            `/api/v1/notifications/${id}`,
            {
                method: "DELETE",
                headers: {
                    Authorization: `Bearer ${accessToken}`
                }
            }
        );

        if (!response.ok) {
            throw new Error(`Lỗi xóa thông báo ${id}: ${response.status}`);
        }

        return response.status !== 204 ? await response.json() : true;
    } catch (error) {
        console.error(`Không thể xóa thông báo với ID ${id}:`, error);
        throw error;
    }
}

// authApi.js (hoặc file quản lý API chung)

export async function refreshTokenApi() {
    try {
        const response = await fetch("/api/v1/auth/refresh", {
            method: "POST",
            credentials: "include",
            headers: {
                "Content-Type": "application/json"
            }
        });
        if (!response.ok) {
            return false;
        }
        const result = await response.json();
        return result;
    } catch (error) {
        console.error("Lỗi làm mới token:", error);
        return false;
    }
}