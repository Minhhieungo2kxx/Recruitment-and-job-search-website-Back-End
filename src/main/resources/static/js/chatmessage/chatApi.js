

export async function refreshAccessToken(app) {
    if (app.isRefreshing && app.refreshPromise) {
        return app.refreshPromise;
    }

    app.isRefreshing = true;

    app.refreshPromise = (async () => {
        try {
            const response = await fetch("/api/v1/auth/refresh", {
                method: "POST",
                credentials: "include", // CỰC KỲ QUAN TRỌNG
                headers: { "Content-Type": "application/json" }
            });

            if (!response.ok) return false;

            const result = await response.json();
            if (result.statusCode !== 200 || !result.data?.access_token) {
                return false;
            }

            app.accessToken = result.data.access_token;
            localStorage.setItem("accessToken", app.accessToken);

            if (result.data.user) {
                app.currentUser = result.data.user;
                localStorage.setItem("userInfo", JSON.stringify(app.currentUser));
            }

            console.log("Refresh accessToken thành công");

            // RECONNECT WS SAU KHI REFRESH
            if (app.stompClient?.connected) {
                app.stompClient.disconnect(() => {
                    app.connectWebSocket();
                });
            }

            return true;
        } catch (err) {
            console.error("Refresh token thất bại", err);
            return false;
        } finally {
            app.isRefreshing = false;
            app.refreshPromise = null;
        }
    })();

    return app.refreshPromise;
}
export async function logout() {
    try {
        const accessToken = localStorage.getItem('accessToken');

        await fetch('/api/v1/auth/logout', {
            method: 'POST',
            credentials: 'include',
            headers: {
                'Authorization': `Bearer ${accessToken}`
            }
        });
    } catch (error) {
        console.error("Logout error:", error);
    } finally {
        // xóa SAU khi gọi API
        localStorage.removeItem('accessToken');
        localStorage.removeItem('userInfo');

        window.location.href = '/';
    }
}
export async function uploadFile(app, file) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', 'chat-files');

    try {
        const response = await fetch('/api/v1/file/cloudinary', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${app.accessToken}`
            },
            body: formData
        });

        if (response.status === 401 || response.status === 403) {
            const refreshed = await refreshAccessToken(app);
            if (!refreshed) {
                await logout();
                return null;
            }

            return await uploadFile(app, file);
        }

        const result = await response.json();
        if (result.statusCode === 200) {
            return result.data;
        }
        return null;
    } catch (error) {
        console.error('Upload file error:', error);
        return null;
    }
}

export async function apiCall(app, url, options = {}) {
    let accessToken = app.accessToken || localStorage.getItem("accessToken");

    const finalOptions = {
        ...options,
        credentials: "include", //  BẮT BUỘC
        headers: {
            ...(options.headers || {}),
            "Authorization": `Bearer ${accessToken}`
        }
    };

    //  CHỈ set Content-Type khi có body JSON
    if (options.body && !(options.body instanceof FormData)) {
        finalOptions.headers["Content-Type"] = "application/json";
    }

    let response = await fetch(url, finalOptions);

    if (response.status === 401 || response.status === 403) {
        console.warn("API 401/403 → refresh token");

        const refreshed = await refreshAccessToken(app);
        if (!refreshed) {
            await logout();
            return null;
        }

        finalOptions.headers.Authorization = `Bearer ${app.accessToken}`;
        response = await fetch(url, finalOptions);
    }

    try {
        return await response.json();
    } catch {
        return null;
    }
}
export async function fetchUserPresence(app, userId) {
    try {
        const response = await apiCall(app, `/api/presence/user/${userId}`);
        if (response) {
            app.handlePresenceUpdate(response);
            return response;
        }
    } catch (error) {
        console.error('Error fetching user presence:', error);
    }
}

export async function loadConversations(app) {
    try {
        const response = await apiCall(app, '/api/v1/messages/conversations');
        if (response.success) {
            app.renderConversations(response.data);
        }
    } catch (error) {
        console.error('Load conversations error:', error);
    }
}

export async function fetchMultipleUsersPresence(app, userIds) {
    try {
        const params = new URLSearchParams();
        userIds.forEach(id => params.append('userIds', id));

        const response = await apiCall(app, `/api/presence/users?${params}`);
        if (response) {
            response.forEach(presence => {
                app.handlePresenceUpdate(presence);
            });
        }
    } catch (error) {
        console.error('Error fetching users presence:', error);
    }
}
export async function loadMessages(app, userId) {
    try {
        const response = await apiCall(app, `/api/v1/messages/conversation/${userId}`);
        if (response.success) {
            app.renderMessages(response.data);
        }
    } catch (error) {
        console.error('Load messages error:', error);
    }
}
export async function sendMessage(app, content) {
    if (!app.currentConversation || !content.trim()) return;

    try {
        const response = await apiCall(app, '/api/v1/messages/send', {
            method: 'POST',
            body: JSON.stringify({
                receiverId: app.currentConversation.id,
                content: content.trim(),
                type: 'CHAT'
            })
        });

        if (response.success) {
            // Message will be added via WebSocket
            document.getElementById('messageInput').value = '';
        }
    } catch (error) {
        console.error('Send message error:', error);
    }
}

export async function sendMessageWithFile(app, content, fileData) {
    if (!app.currentConversation) return;

    try {
        const response = await apiCall(app, '/api/v1/messages/send', {
            method: 'POST',
            body: JSON.stringify({
                receiverId: app.currentConversation.id,
                content: content.trim() || 'Đã gửi file',
                type: 'CHAT',
                contentType: fileData.contentType,
                fileUrl: fileData.fileName,
                fileName: fileData.originalName,
                fileSize: fileData.fileSize,
                publicId:fileData.publicId
            })
        });

        if (response.success) {
            document.getElementById('messageInput').value = '';
            app.uploadedFiles = [];
            app.hideFilePreview();
        }
    } catch (error) {
        console.error('Send message with file error:', error);
    }
}

export async function saveEditedMessage(app) {
    const newContent = document.getElementById('editMessageText').value.trim();
    if (!newContent || !app.editingMessageId) return;

    try {
        const response = await apiCall(app, '/api/v1/messages/update', {
            method: 'PUT',
            body: JSON.stringify({
                messageId: app.editingMessageId,
                content: newContent
            })
        });

        if (response.success) {
            bootstrap.Modal.getInstance(document.getElementById('editMessageModal')).hide();
            app.editingMessageId = null;
        }
    } catch (error) {
        console.error('Edit message error:', error);
    }
}

export async function deleteMessage(app, messageId) {
    if (!confirm('Bạn có chắc muốn xóa tin nhắn này?')) return;

    try {
        const response = await apiCall(app, `/api/v1/messages/${messageId}`, {
            method: 'DELETE'
        });

        if (response.success) {

        }
    } catch (error) {
        console.error('Delete message error:', error);
    }
}

export async function searchUsers(app, searchTerm) {
    if (!searchTerm.trim()) {
        document.getElementById('userSearchResults').style.display = 'none';
        return;
    }

    try {
        const response = await apiCall(app, `/api/v1/messages/search-users?searchTerm=${encodeURIComponent(searchTerm)}`);
        if (response.success) {
            app.renderUserSearchResults(response.data);
        }
    } catch (error) {
        console.error('Search users error:', error);
    }
}



export async function downloadCloudFile(app,encodedUrl,originalName) {
    try {
        const response = await fetch(
            `/api/v1/download/cloud/${encodedUrl}`,
            {
                method: 'GET',
                headers: {
                    Authorization: `Bearer ${app.accessToken}`
                }
            }
        );

        if (response.status === 401 || response.status === 403) {
            const refreshed = await refreshAccessToken(app);
            if (refreshed) {
                return downloadCloudFile(app,encodedUrl,originalName);
            }
            else {
                alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                await logout();
                return;
            }
        }

        if (!response.ok) {
            alert("Không thể tải file");
            return;
        }

        const blob = await response.blob();

        const downloadUrl = window.URL.createObjectURL(blob);

        const a = document.createElement("a");
        a.href = downloadUrl;
        a.download = originalName;

        document.body.appendChild(a);
        a.click();

        a.remove();
        window.URL.revokeObjectURL(downloadUrl);

    } catch (error) {
        console.error("Download Cloudinary error:", error);
    }
}