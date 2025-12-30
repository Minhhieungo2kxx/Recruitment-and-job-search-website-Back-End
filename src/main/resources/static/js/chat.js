class ChatApp {
    constructor() {
        this.stompClient = null;
        this.currentUser = null;
        this.currentConversation = null;
        this.editingMessageId = null;
        this.toastTimeInterval = null;
        // Thêm cho presence system
        this.userPresences = new Map();
        this.activeToasts = new Map(); // khởi tạo ở constructor
        this.presenceUpdateInterval = null;
        this.isRefreshing = false;
        this.refreshPromise = null;
        this.accessToken = null;
        this.MAX_WS_RETRY = 3;
        this.uploadedFiles = []; // Lưu file đã chọn
        this.init();
    }
    async init() {
        // Lấy token hiện có (có thể đã cũ)
        this.accessToken = localStorage.getItem('accessToken');

        this.currentUser = JSON.parse(localStorage.getItem('userInfo'));
        if (!this.currentUser || !this.accessToken) {
            logout();
            return;
        }
        const avatarContainer = document.getElementById('sidebarUserAvatar');
        if (avatarContainer && this.currentUser) {
            avatarContainer.innerHTML = '';

            if (this.currentUser.avatar) {
                const img = document.createElement('img');
                img.src = 'http://localhost:8081/storage/user/' + this.currentUser.avatar;
                img.alt = this.currentUser.fullName;
                img.className = 'avatar-img';
                avatarContainer.appendChild(img);
            } else {
                avatarContainer.textContent = this.getInitials(this.currentUser.fullName);
                avatarContainer.classList.add('initials');
            }
        }

        // UI
        document.getElementById('userFullName').textContent =
            this.currentUser.fullName;

        // KHÔNG refresh ở đây
        await this.connectWebSocket();

        // Các API phía dưới sẽ tự refresh nếu cần
        await this.loadConversations();

        this.setupEventListeners();
        this.startPresenceUpdates();
    }
    async connectWebSocket(retryCount = 0) {
        //  KIỂM TRA TOKEN TRƯỚC
        if (this.isAccessTokenExpired()) {
            console.warn("Access token expired → refresh BEFORE WS connect");

            const ok = await this.refreshAccessToken();
            if (!ok) {
                logout();
                return;
            }
        }

        return new Promise((resolve, reject) => {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);

            this.stompClient.connect(
                {
                    Authorization: `Bearer ${localStorage.getItem("accessToken")}`,
                    userId: this.currentUser.id
                },
                () => {
                   console.log(" WS Connected");
                   this.subscribeWS();
                   this.stompClient.ws.onclose = () => {
                       console.warn("WS closed → reconnect");
                       setTimeout(() => this.connectWebSocket(), 2000);
                   };

                    resolve();
                },
                async (error) => {
                    console.warn(" WS connect error", error);

                    // ❗ CONNECT FAIL → REFRESH NGAY
                    const ok = await this.refreshAccessToken();
                    if (ok) {
                        console.log("Retry WS after refresh");
                        this.connectWebSocket(0);
                    } else {
                        logout();
                        reject(error);
                    }
                }
            );
        });
    }
    isAccessTokenExpired() {
        const token = localStorage.getItem("accessToken");
        if (!token) return true;

        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const now = Math.floor(Date.now() / 1000);
            return payload.exp <= now + 5;
        } catch (e) {
            return true;
        }
    }


    subscribeWS() {
        this.stompClient.subscribe('/user/queue/messages', msg => {
            this.handleNewMessage(JSON.parse(msg.body));
        });

        this.stompClient.subscribe('/user/queue/message-updates', msg => {
            this.handleMessageUpdate(JSON.parse(msg.body));
        });
        this.stompClient.subscribe('/user/queue/message-status', msg => {
            this.handleMessageStatusUpdate(JSON.parse(msg.body));
        });

        this.stompClient.subscribe('/user/queue/typing', msg => {
            const data = JSON.parse(msg.body);
            if (this.currentConversation && data.senderId === this.currentConversation.id) {
                document.getElementById('typingIndicator').style.display =
                    data.typing ? 'block' : 'none';
            }
        });

        this.stompClient.subscribe('/user/queue/message-deletes', msg => {
            this.handleMessageDelete(JSON.parse(msg.body));
        });

        this.stompClient.subscribe('/topic/presence', msg => {
            this.handlePresenceUpdate(JSON.parse(msg.body));
        });


    }
    }

    increaseUnread(userId) {
        const convItem = document.querySelector(
            `.conversation-item[data-user-id="${userId}"]`
        );

        if (!convItem) return;

        let unreadEl = convItem.querySelector('.unread-count');

        if (!unreadEl) {
            unreadEl = document.createElement('div');
            unreadEl.className = 'unread-count';
            unreadEl.textContent = '1';
            convItem.querySelector('.conversation-meta').prepend(unreadEl);
        } else {
            unreadEl.textContent = parseInt(unreadEl.textContent) + 1;
        }
    }
    moveConversationToTop(userId) {
        const convItem = document.querySelector(
            `.conversation-item[data-user-id="${userId}"]`
        );

        if (!convItem) return;

        const container = document.getElementById('conversationsList');
        container.prepend(convItem);
    }
    // Thêm method xử lý upload file
    async uploadFile(file) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('folder', 'chat-files');

        try {
            const response = await fetch('/api/v1/file/server', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                },
                body: formData
            });

            if (response.status === 401 || response.status === 403) {
                const refreshed = await this.refreshAccessToken();
                if (!refreshed) {
                    logout();
                    return null;
                }

                return await this.uploadFile(file);
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


// XỬ LÝ CẬP NHẬT PRESENCE
    handlePresenceUpdate(presence)    {
        this.userPresences.set(presence.userId, presence);
        this.updateUserStatusUI(presence);
    }

    // CẬP NHẬT UI TRẠNG THÁI
    updateUserStatusUI(presence) {
        const { userId, isOnline, statusText, statusType, lastSeenAt } = presence;

        // Cập nhật trong danh sách conversation
        const convItem = document.querySelector(`[data-user-id="${userId}"]`);
        if (convItem) {
            // Cập nhật tên với màu sắc
            const nameEl = convItem.querySelector('.conversation-name');
            if (nameEl) {
                // Giữ nguyên tên, chỉ thêm indicator
                const currentName = nameEl.textContent.replace(/\s*●.*/, ''); // Xóa indicator cũ nếu có
                nameEl.innerHTML = `${currentName} ${this.getStatusIndicator(statusType)}`;
            }
            // Thêm/cập nhật status text
            let statusEl = convItem.querySelector('.user-status-text');
            if (!statusEl) {
                statusEl = document.createElement('div');
                statusEl.className = 'user-status-text';
                const infoEl = convItem.querySelector('.conversation-info');
                if (infoEl) {
                    infoEl.appendChild(statusEl);
                }
            }
            statusEl.textContent = statusText;
            statusEl.className = `user-status-text status-${statusType}`;



            // Cập nhật avatar indicator
            let avatarIndicator = convItem.querySelector('.avatar-status-indicator');
            if (!avatarIndicator) {
                avatarIndicator = document.createElement('span');
                avatarIndicator.className = 'avatar-status-indicator';
                const avatarEl = convItem.querySelector('.avatar');
                if (avatarEl && avatarEl.style.position !== 'relative') {
                    avatarEl.style.position = 'relative';
                    avatarEl.appendChild(avatarIndicator);
                }
            }
            avatarIndicator.className = `avatar-status-indicator indicator-${statusType}`;
        }

        // Cập nhật header nếu đang chat với user này
        if (this.currentConversation && this.currentConversation.id === userId) {
            this.updateChatHeaderStatus(statusText, statusType);
        }
    }

    // Lấy indicator cho status
    getStatusIndicator(statusType) {
        switch(statusType) {
            case 'online':
                return '<span style="color: #4CAF50;">●</span>';
            case 'recently':
                return '<span style="color: #FFC107;">●</span>';
            case 'away':
                return '<span style="color: #FF9800;">●</span>';
            default:
                return '<span style="color: #9E9E9E;">●</span>';
        }
    }

    // Cập nhật header chat với status
    updateChatHeaderStatus(statusText, statusType) {
        const statusEl = document.getElementById('chatUserStatus');
        if (statusEl) {
            statusEl.innerHTML = `
                ${this.getStatusIndicator(statusType)} ${statusText}
            `;
            statusEl.className = `status-${statusType}`;
        }
    }

    // Fetch presence từ API
    async fetchUserPresence(userId) {
        try {
            const response = await this.apiCall(`/api/presence/user/${userId}`);
            if (response) {
                this.handlePresenceUpdate(response);
                return response;
            }
        } catch (error) {
            console.error('Error fetching user presence:', error);
        }
    }

    // Fetch nhiều users presence
    async fetchMultipleUsersPresence(userIds) {
        try {
            const params = new URLSearchParams();
            userIds.forEach(id => params.append('userIds', id));

            const response = await this.apiCall(`/api/presence/users?${params}`);
            if (response) {
                response.forEach(presence => {
                    this.handlePresenceUpdate(presence);
                });
            }
        } catch (error) {
            console.error('Error fetching users presence:', error);
        }
    }

    // Cập nhật presence định kỳ
    startPresenceUpdates() {
        if (this.presenceUpdateInterval) clearInterval(this.presenceUpdateInterval);

        this.presenceUpdateInterval = setInterval(async () => {
            for (let [userId, presence] of this.userPresences) {
                try {
                    // Lấy trạng thái thực từ server
                    const updatedPresence = await this.fetchUserPresence(userId);

                    // Update UI nếu khác hiện tại
                    if (updatedPresence.statusType !== presence.statusType ||
                        updatedPresence.statusText !== presence.statusText) {

                        this.userPresences.set(userId, updatedPresence);
                        this.updateUserStatusUI(updatedPresence);
                    }
                } catch (err) {
                    console.error('Lỗi fetch presence:', err);
                }
            }
        }, 50000); // mỗi 50s ~ 1 phút
    }



    // Format thời gian tương đối tiếng Việt
    formatRelativeTime(lastSeenAt) {
        if (!lastSeenAt) return 'Chưa từng truy cập';

        let lastSeen;

        // Nếu là số (epoch giây), chuyển thành milliseconds
        if (typeof lastSeenAt === 'number') {
            lastSeen = new Date(lastSeenAt * 1000); // Nhân 1000 ở đây
        } else if (typeof lastSeenAt === 'string') {
            lastSeen = new Date(lastSeenAt);
        } else {
            return 'Không rõ thời gian truy cập';
        }

        if (isNaN(lastSeen.getTime())) {
            return 'Không rõ thời gian truy cập';
        }

        const now = new Date();
        const diffMs = now - lastSeen;
        const diffSec = Math.floor(diffMs / 1000);
        const diffMin = Math.floor(diffSec / 60);
        const diffHour = Math.floor(diffMin / 60);
        const diffDay = Math.floor(diffHour / 24);
        const diffWeek = Math.floor(diffDay / 7);
        const diffMonth = Math.floor(diffDay / 30);
        const diffYear = Math.floor(diffDay / 365);

        if (diffSec < 60) {
            return 'Vừa truy cập';
        } else if (diffMin < 5) {
            return `Hoạt động ${diffMin} phút trước`;
        } else if (diffMin < 60) {
            return `Hoạt động ${diffMin} phút trước`;
        } else if (diffHour < 24) {
            return `Hoạt động ${diffHour} giờ trước`;
        } else if (diffDay < 7) {
            return `Hoạt động ${diffDay} ngày trước`;
        } else if (diffWeek < 4) {
            return `Hoạt động ${diffWeek} tuần trước`;
        } else if (diffMonth < 12) {
            return `Hoạt động ${diffMonth} tháng trước`;
        } else {
            return `Hoạt động ${diffYear} năm trước`;
        }
    }


    // CẬP NHẬT METHOD renderConversations
    renderConversations(conversations) {
        const container = document.getElementById('conversationsList');
        container.innerHTML = '';

        // Lấy presence cho tất cả users
        const userIds = conversations.map(c => c.otherUser.id);
        this.fetchMultipleUsersPresence(userIds);

        conversations.forEach(conversation => {
            const user = conversation.otherUser;
            const div = document.createElement('div');
            div.className = 'conversation-item';
            div.dataset.userId = user.id;

            const lastMessageText = conversation.lastMessage
                ? conversation.lastMessage.content
                : 'Chưa có tin nhắn';

            // Avatar HTML (giống selectConversation)
            let avatarHtml = '';
            if (user.avatar) {
                avatarHtml = `
                    <img
                        src="http://localhost:8081/storage/user/${user.avatar}"
                        alt="${user.fullName}"
                        class="avatar-img"
                    />
                `;
            } else {
                avatarHtml = this.getInitials(user.fullName);
            }

            div.innerHTML = `
                <div class="avatar">
                    ${avatarHtml}
                    <span class="avatar-status-indicator"></span>
                </div>

                <div class="conversation-info">
                    <div class="conversation-name">${user.fullName}</div>
                    <div class="user-status-text"></div>
                    <div class="last-message">${lastMessageText}</div>
                </div>

                <div class="conversation-meta">
                    ${
                        conversation.unreadCount > 0
                            ? `<div class="unread-count">${conversation.unreadCount}</div>`
                            : ''
                    }
                </div>
            `;

            div.addEventListener('click', () => this.selectConversation(user));
            container.appendChild(div);
        });
    }

    // CẬP NHẬT METHOD selectConversation
    async selectConversation(user) {
        this.currentConversation = user;
        const presence = this.userPresences.get(user.id);
        if (presence) {
            presence.isOnline = true;
            presence.statusType = 'online';
            presence.statusText = 'Đang hoạt động';
            this.updateUserStatusUI(presence);
        }


        // Update UI
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
        });

        const element = document.querySelector(`[data-user-id="${user.id}"]`);
        if (element) {
            element.classList.add('active');
        }

        const emptyChat = document.getElementById('emptyChat');
        if (emptyChat) emptyChat.style.display = 'none';

        const chatHeader = document.getElementById('chatHeader');
        if (chatHeader) chatHeader.style.display = 'flex';

        const inputContainer = document.getElementById('messageInputContainer');
        if (inputContainer) inputContainer.style.display = 'block';

        const chatUserNameEl = document.getElementById('chatUserName');
        if (chatUserNameEl) {
            chatUserNameEl.textContent = user.fullName;
        }

        const chatUserAvatarEl = document.getElementById('chatUserAvatar');
        if (chatUserAvatarEl) {
            chatUserAvatarEl.innerHTML = '';
            if (user.avatar) {
                const img = document.createElement('img');
                img.src = 'http://localhost:8081/storage/user/' + user.avatar;
                img.alt = user.fullName;
                img.className = 'avatar-img';
                chatUserAvatarEl.appendChild(img);
            } else {
                chatUserAvatarEl.textContent = this.getInitials(user.fullName);
            }
        }

        const chatUser = document.getElementById('chatUser-Search');
        if (chatUser) {
            chatUser.innerHTML = '';
            if (user.avatar) {
                const img = document.createElement('img');
                img.src = 'http://localhost:8081/storage/user/' + user.avatar;
                img.alt = user.fullName;
                img.className = 'avatar-img';
                chatUser.appendChild(img);
            } else {
                chatUser.textContent = this.getInitials(user.fullName);
            }
        }

        // Fetch và hiển thị presence status
        const chatUserStatusEl = document.getElementById('chatUserStatus');
        if (chatUserStatusEl) {
            chatUserStatusEl.textContent = "Đang kiểm tra...";
        }

        try {
            const presence = await this.fetchUserPresence(user.id);
            if (presence) {
                this.updateChatHeaderStatus(presence.statusText, presence.statusType);
            }
        } catch (err) {
            console.error("Check status error:", err);
            if (chatUserStatusEl) {
                chatUserStatusEl.textContent = "Offline";
                chatUserStatusEl.className = "text-muted";
            }
        }
        // Reset unread count
        const convItem = document.querySelector(
            `.conversation-item[data-user-id="${user.id}"]`
        );
        if (convItem) {
            const unreadEl = convItem.querySelector('.unread-count');
            if (unreadEl) unreadEl.remove();
        }
        const unreadEl = convItem.querySelector('.unread-count');
        if (unreadEl) unreadEl.remove();

        // Load messages
        await this.loadMessages(user.id);
        //  GỬI SEEN CHO CÁC TIN CHƯA ĐỌC
        this.markMessagesAsSeen(user.id);
    }
    markMessagesAsSeen(otherUserId) {
        const messages = document.querySelectorAll(
            `.message:not(.own)`
        );

        messages.forEach(msgEl => {
            const messageId = msgEl.dataset.messageId;

            if (this.stompClient && this.stompClient.connected) {
                this.stompClient.send(
                    "/app/chat.seen",
                    {},
                    JSON.stringify({
                        messageId: Number(messageId),
                        senderId: otherUserId,
                        receiverId: this.currentUser.id
                    })
                );
            }
        });
    }



    updateUserStatus(userId, isOnline) {
        // Nếu đang trong cuộc trò chuyện
        // Cập nhật trong danh sách conversation
        const convEl = document.querySelector(`[data-user-id="${userId}"] .conversation-name`);
        if (convEl) {
            convEl.style.color = isOnline ? "green" : "gray";
        }
        // Header của đoạn chat đang mở
        if (this.currentConversation && this.currentConversation.id === userId) {
            const el = document.getElementById('chatUserStatus');
            if (el) {
                el.textContent = isOnline ? 'Online' : 'Offline';
                el.className = isOnline ? 'text-success' : 'text-muted';
            }
        }


    }
    async apiCall(url, options = {}) {
        let accessToken = this.accessToken || localStorage.getItem("accessToken");

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

            const refreshed = await this.refreshAccessToken();
            if (!refreshed) {
                logout();
                return null;
            }

            finalOptions.headers.Authorization = `Bearer ${this.accessToken}`;
            response = await fetch(url, finalOptions);
        }

        try {
            return await response.json();
        } catch {
            return null;
        }
    }


    // ===== REFRESH ACCESS TOKEN =====
    async refreshAccessToken() {
        if (this.isRefreshing && this.refreshPromise) {
            return this.refreshPromise;
        }

        this.isRefreshing = true;

        this.refreshPromise = (async () => {
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

                this.accessToken = result.data.access_token;
                localStorage.setItem("accessToken", this.accessToken);

                if (result.data.user) {
                    this.currentUser = result.data.user;
                    localStorage.setItem("userInfo", JSON.stringify(this.currentUser));
                }

                console.log("Refresh accessToken thành công");

                //  RECONNECT WS SAU KHI REFRESH
                if (this.stompClient?.connected) {
                    this.stompClient.disconnect(() => {
                        this.connectWebSocket();
                    });
                }

                return true;
            } catch (err) {
                console.error(" Refresh token thất bại", err);
                return false;
            } finally {
                this.isRefreshing = false;
                this.refreshPromise = null;
            }
        })();

        return this.refreshPromise;
    }



    async loadConversations() {
        try {
            const response = await this.apiCall('/api/v1/messages/conversations');
            if (response.success) {
                this.renderConversations(response.data);
            }
        } catch (error) {
            console.error('Load conversations error:', error);
        }
    }


    async loadMessages(userId) {
        try {
            const response = await this.apiCall(`/api/v1/messages/conversation/${userId}`);
            if (response.success) {
                this.renderMessages(response.data);
            }
        } catch (error) {
            console.error('Load messages error:', error);
        }
    }

    renderMessages(messages) {
        const container = document.getElementById('chatMessages');
        container.innerHTML = '';

        messages.forEach(message => {
            const messageDiv = this.createMessageElement(message);
            container.appendChild(messageDiv);
        });

        // Scroll to bottom
        container.scrollTop = container.scrollHeight;
    }
    // Render message có file
    createMessageElement(message) {
        const div = document.createElement('div');
        const isMine = message.sender.id === this.currentUser.id;
        div.className = `message ${isMine ? 'own' : ''}`;
        div.dataset.messageId = message.id;

        const time = this.formatTime(message.createdAt);
        const editedText = message.isEdited ? ' (đã sửa)' : '';

        const avatarHtml = message.sender.avatar
            ? `<img src="http://localhost:8081/storage/user/${message.sender.avatar}" class="message-avatar" />`
            : `<div class="message-avatar initials">${this.getInitials(message.sender.fullName)}</div>`;

        // Xử lý nội dung file
        let fileContentHtml = '';
        if (message.contentType === 'IMAGE') {
            fileContentHtml = `
                <div class="message-file-content">
                    <img src="http://localhost:8081/storage/chat-files/${message.fileUrl}"
                         class="message-image"
                         onclick="chatApp.viewImage('http://localhost:8081/storage/chat-files/${message.fileUrl}')" />
                </div>
            `;
        } else if (message.contentType === 'FILE') {
              fileContentHtml = `
                  <div class="message-file-content">
                      <div class="file-attachment">
                          <i class="fas fa-file-pdf fa-2x text-danger"></i>
                          <div class="file-info">
                              <div class="file-name">${message.fileName || 'file'}</div>
                              <button class="btn btn-sm btn-primary mt-1"
                                  onclick="chatApp.downloadFile(
                                      'chat-files',
                                      '${message.fileUrl}',
                                      '${message.fileName || message.fileUrl}'
                                  )">
                                  <i class="fas fa-download"></i> Tải xuống
                              </button>
                          </div>
                      </div>
                  </div>
              `;
          }



        div.innerHTML = `
            ${!isMine ? `<div class="message-avatar-container">${avatarHtml}</div>` : ''}

            <div class="message-bubble ${isMine ? 'bubble-mine' : 'bubble-other'}">
                ${fileContentHtml}

                ${message.content && message.content !== 'Đã gửi file'
                    ? `<div class="message-content">${message.content}</div>`
                    : ''}

                <div class="message-time-status">
                    <span class="message-time">${time}${editedText}</span>
                    ${isMine ? (message.status === 'READ'
                        ? `<span class="message-seen-avatar">${this.renderSeenAvatar(message)}</span>`
                        : `<span class="message-status">${this.getStatusText(message.status)}</span>`)
                        : ''}
                </div>

                ${
                  isMine
                    ? `<div class="message-actions">
                          <button class="btn btn-sm btn-outline-secondary me-1"
                                  onclick="chatApp.editMessage(${message.id}, '${message.content}')">
                              <i class="fas fa-edit"></i>
                          </button>
                          <button class="btn btn-sm btn-outline-danger"
                                  onclick="chatApp.deleteMessage(${message.id})">
                              <i class="fas fa-trash"></i>
                          </button>
                          </div>`
                    : ''
                }

            </div>
        `;

        return div;
    }
    async downloadFile(folder, fileUrl, originalName) {
        try {
            const response = await fetch(
                `/api/v1/download/${folder}/${fileUrl}`,
                {
                    method: 'GET',
                    headers: {
                        Authorization: `Bearer ${this.accessToken}`
                    }
                }
            );

            //  NÂNG CẤP: refresh token khi 401 / 403
            if (response.status === 401 || response.status === 403) {
                const refreshed = await this.refreshAccessToken();
                if (refreshed) {
                    // retry 1 lần với token mới
                    return this.downloadFile(folder, fileUrl, originalName);
                } else {
                    alert('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
                    logout();
                    return;
                }
            }

            if (!response.ok) {
                alert('Không thể tải file');
                return;
            }

            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);

            const a = document.createElement('a');
            a.href = downloadUrl;
            a.download = originalName || fileUrl;
            document.body.appendChild(a);
            a.click();

            a.remove();
            window.URL.revokeObjectURL(downloadUrl);

        } catch (error) {
            console.error('Download file error:', error);
            alert('Lỗi khi tải file');
        }
    }

    // Xem ảnh fullscreen
    viewImage(imageUrl) {
        const modal = document.getElementById('imageViewModal');
        const modalImg = document.getElementById('imageViewContent');

        modal.style.display = 'block';
        modalImg.src = imageUrl;
    }

    closeImageView() {
        document.getElementById('imageViewModal').style.display = 'none';
    }


    getStatusText(status) {
        switch (status) {
            case 'SENT':
                return '✔ Đã gửi';
            case 'READ':
                return ''; // READ sẽ hiển thị avatar, không dùng text
            default:
                return '';
        }
    }
    renderSeenAvatar(message) {
        const receiver = message.receiver;

        if (receiver.avatar) {
            return `
                <img src="http://localhost:8081/storage/user/${receiver.avatar}"
                     class="seen-avatar"
                     title="Đã xem" />
            `;
        }
        return `
            <div class="seen-avatar initials">
                ${this.getInitials(receiver.fullName)}
            </div>
        `;
    }


    async sendMessage(content) {
        if (!this.currentConversation || !content.trim()) return;

        try {
            const response = await this.apiCall('/api/v1/messages/send', {
                method: 'POST',
                body: JSON.stringify({
                    receiverId: this.currentConversation.id,
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
//    Gửi tin nhắn có file
    async sendMessageWithFile(content, fileData) {
        if (!this.currentConversation) return;

        try {
            const response = await this.apiCall('/api/v1/messages/send', {
                method: 'POST',
                body: JSON.stringify({
                    receiverId: this.currentConversation.id,
                    content: content.trim() || 'Đã gửi file',
                    type: 'CHAT',
                    contentType: fileData.contentType,
                    fileUrl: fileData.fileName,
                    fileName: fileData.originalName,
                    fileSize: fileData.fileSize
                })
            });

            if (response.success) {
                document.getElementById('messageInput').value = '';
                this.uploadedFiles = [];
                this.hideFilePreview();
            }
        } catch (error) {
            console.error('Send message with file error:', error);
        }
    }
    // Hiển thị preview file đã chọn
    showFilePreview(file, fileData) {
        const previewContainer = document.getElementById('filePreviewContainer');
        const preview = document.getElementById('filePreview');

        preview.innerHTML = '';

        const fileItem = document.createElement('div');
        fileItem.className = 'file-preview-item';

        if (file.type.startsWith('image/')) {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(file);
            img.className = 'preview-image';
            fileItem.appendChild(img);
        } else {
            const icon = document.createElement('div');
            icon.className = 'file-icon';
            icon.innerHTML = '<i class="fas fa-file-pdf fa-3x text-danger"></i>';
            fileItem.appendChild(icon);
        }

        const fileName = document.createElement('div');
        fileName.className = 'file-name';
        fileName.textContent = file.name;
        fileItem.appendChild(fileName);

        const removeBtn = document.createElement('button');
        removeBtn.className = 'btn-remove-file';
        removeBtn.innerHTML = '<i class="fas fa-times"></i>';
        removeBtn.onclick = () => this.hideFilePreview();
        fileItem.appendChild(removeBtn);

        preview.appendChild(fileItem);
        previewContainer.style.display = 'block';
    }
    hideFilePreview() {
        const previewContainer = document.getElementById('filePreviewContainer');
        previewContainer.style.display = 'none';
        document.getElementById('fileInput').value = '';
        this.uploadedFiles = [];
    }

    editMessage(messageId, currentContent) {
        this.editingMessageId = messageId;
        document.getElementById('editMessageText').value = currentContent;
        new bootstrap.Modal(document.getElementById('editMessageModal')).show();
    }

    async saveEditedMessage() {
        const newContent = document.getElementById('editMessageText').value.trim();
        if (!newContent || !this.editingMessageId) return;

        try {
            const response = await this.apiCall('/api/v1/messages/update', {
                method: 'PUT',
                body: JSON.stringify({
                    messageId: this.editingMessageId,
                    content: newContent
                })
            });

            if (response.success) {
                bootstrap.Modal.getInstance(document.getElementById('editMessageModal')).hide();
                this.editingMessageId = null;
            }
        } catch (error) {
            console.error('Edit message error:', error);
        }
    }

    async deleteMessage(messageId) {
        if (!confirm('Bạn có chắc muốn xóa tin nhắn này?')) return;

        try {
            const response = await this.apiCall(`/api/v1/messages/${messageId}`, {
                method: 'DELETE'
            });

            if (response.success) {

            }
        } catch (error) {
            console.error('Delete message error:', error);
        }
    }

    async searchUsers(searchTerm) {
        if (!searchTerm.trim()) {
            document.getElementById('userSearchResults').style.display = 'none';
            return;
        }

        try {
            const response = await this.apiCall(`/api/v1/messages/search-users?searchTerm=${encodeURIComponent(searchTerm)}`);
            if (response.success) {
                this.renderUserSearchResults(response.data);
            }
        } catch (error) {
            console.error('Search users error:', error);
        }
    }

    renderUserSearchResults(users) {
        const container = document.getElementById('userSearchResults');
        container.innerHTML = '';

        if (users.length === 0) {
            container.innerHTML =
                '<div class="text-center p-3 text-muted">Không tìm thấy người dùng</div>';
        } else {
            users.forEach(user => {
                const div = document.createElement('div');
                div.className = 'user-search-item';

                // Avatar HTML: có avatar thì dùng ảnh, không có thì dùng initials
                const avatarHtml = user.avatar
                    ? `
                        <img
                            src="http://localhost:8081/storage/user/${user.avatar}"
                            alt="${user.fullName}"
                            class="avatar-img"
                        />
                      `
                    : `
                        <div class="avatar initials">
                            ${this.getInitials(user.fullName)}
                        </div>
                      `;

                div.innerHTML = `
                    <div class="avatar me-3">
                        ${avatarHtml}
                    </div>
                    <div>
                        <div class="fw-semibold">${user.fullName}</div>
                        <small class="text-muted">${user.email}</small>
                    </div>
                `;

                div.addEventListener('click', () => {
                    this.selectConversation(user);
                    document.getElementById('userSearch').value = '';
                    container.style.display = 'none';
                });

                container.appendChild(div);
            });
        }

        container.style.display = 'block';
    }

   handleNewMessage(message) {
       const otherUserId =
           message.sender.id === this.currentUser.id
               ? message.receiver.id
               : message.sender.id;

       const isInCurrentChat =
           this.currentConversation &&
           (message.sender.id === this.currentConversation.id ||
            message.receiver.id === this.currentConversation.id);
        if (isInCurrentChat && message.sender.id !== this.currentUser.id) {
            if (this.stompClient && this.stompClient.connected) {
                this.stompClient.send(
                    "/app/chat.seen",
                    {},
                    JSON.stringify({
                        messageId: message.id,
                        senderId: message.sender.id,
                        receiverId: this.currentUser.id
                    })
                );
            }
        }

       // =====  HIỆN TOAST REALTIME TRÊN GIAO DIỆN =====
       if (!isInCurrentChat && message.sender.id !== this.currentUser.id) {
           this.showRealtimeToast(message);
       }

       // Append message nếu đang chat
       if (isInCurrentChat) {
           const messageElement = this.createMessageElement(message);
           const chatMessages = document.getElementById('chatMessages');
           chatMessages.appendChild(messageElement);
           chatMessages.scrollTop = chatMessages.scrollHeight;
       }
       // Update conversation list
       this.updateConversationItem(message);
   }
   showRealtimeToast(message) {
       const container = document.getElementById('realtimeToastContainer');
       if (!container) return;

       const userId = message.sender.id;
       let toast = this.activeToasts.get(userId);

       const renderContent = (toastEl) => {
           const contentEl = toastEl.querySelector('.toast-message');
           const badgeEl = toastEl.querySelector('.toast-badge');
           const timeEl = toastEl.querySelector('.toast-time');

           const messages = toastEl.messages;
           contentEl.textContent = messages[messages.length - 1];

           timeEl.textContent = this.formatToastTime(toastEl.lastTime);

           if (messages.length > 1) {
               badgeEl.textContent = messages.length;
               badgeEl.style.display = 'flex';
           } else {
               badgeEl.style.display = 'none';
           }
       };

       if (toast) {
           toast.messages.push(message.content);
           toast.lastTime = message.createdAt || Date.now();
           renderContent(toast);
           container.prepend(toast);
           return;
       }

       toast = document.createElement('div');
       toast.className = 'realtime-toast zalo-style';
       toast.messages = [message.content];
       toast.lastTime = message.createdAt || Date.now();

       const avatarHTML = message.sender.avatar
           ? `<img src="http://localhost:8081/storage/user/${message.sender.avatar}"
                    class="toast-avatar" />`
           : `<div class="toast-avatar initials">
                  ${this.getInitials(message.sender.fullName)}
              </div>`;

       toast.innerHTML = `
           ${avatarHTML}
           <div class="toast-body">
               <div class="toast-header">
                   <span class="toast-sender">${message.sender.fullName}</span>
                   <span class="toast-time"></span>
               </div>
               <div class="toast-message"></div>
               <span class="toast-badge" style="display:none"></span>
           </div>
       `;

       renderContent(toast);

       // Khi click → mở chat và xóa toast
       toast.addEventListener('click', () => {
           this.selectConversation(message.sender);
           toast.remove();
           this.activeToasts.delete(userId);
       });

       container.prepend(toast);
       this.activeToasts.set(userId, toast);

       // Bắt đầu updater thời gian (auto cập nhật 1 phút/lần)
       this.startToastTimeUpdater();


   }


    handleMessageUpdate(message) {
        const messageElement = document.querySelector(`[data-message-id="${message.id}"]`);
        if (messageElement) {
            const contentElement = messageElement.querySelector('.message-content');
            const timeElement = messageElement.querySelector('.message-time');
            timeElement.textContent = this.formatTime(message.updatedAt) + ' (đã sửa)';
            contentElement.textContent = message.content;

        }
    }
    handleMessageStatusUpdate(message) {
        const el = document.querySelector(`[data-message-id="${message.id}"]`);
        if (!el) return;

        const statusContainer = el.querySelector('.message-time-status');
        if (!statusContainer) return;

        if (message.status === 'READ') {
            statusContainer.innerHTML = `
                <span class="message-time">${this.formatTime(message.createdAt)}</span>
                <span class="message-seen-avatar">
                    ${this.renderSeenAvatar(message)}
                </span>
            `;
        } else {
            const statusEl = el.querySelector('.message-status');
            if (statusEl) {
                statusEl.textContent = this.getStatusText(message.status);
            }
        }
    }



    handleMessageDelete(deleteInfo) {
        const messageElement = document.querySelector(
            `[data-message-id="${deleteInfo.messageId}"]`
        );
        if (!messageElement) return;

        const bubble = messageElement.querySelector('.message-bubble');
        if (!bubble) return;

        const isDeletedByMe = deleteInfo.deletedByUserId === this.currentUser.id;

        const text = isDeletedByMe
            ? 'Bạn đã xóa tin nhắn này'
            : 'Người kia đã xóa tin nhắn này';

       bubble.innerHTML = `
           <div class="message-content text-dark fst-italic text-center">
               ${text}
           </div>
       `;

        messageElement.classList.add('deleted-message');
    }




    setupEventListeners() {

        // Message form
        document.getElementById('messageForm').addEventListener('submit', (e) => {
            e.preventDefault();
            const input = document.getElementById('messageInput');
            this.sendMessage(input.value);
        });

        // User search
        const searchInput = document.getElementById('userSearch');
        let searchTimeout;

        searchInput.addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                this.searchUsers(e.target.value);
            }, 300);
        });

        // Hide search results when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-container')) {
                document.getElementById('userSearchResults').style.display = 'none';
            }
        });


        // Xử lý sự kiện khi người dùng nhập tin nhắn
        let typingTimeout;
        const messageInput = document.getElementById('messageInput');
        messageInput.addEventListener('input', () => {
            const typingIndicator = document.getElementById('typingIndicator');
            if (this.currentConversation) {
                if (this.stompClient) {
                    this.stompClient.send('/app/chat.typing', {}, JSON.stringify({
                        receiverId: this.currentConversation.id,
                        typing: true
                    }));
                }

                typingIndicator.style.display = 'block';

                clearTimeout(typingTimeout);
                typingTimeout = setTimeout(() => {
                    if (this.stompClient) {
                        this.stompClient.send('/app/chat.typing', {}, JSON.stringify({
                            receiverId: this.currentConversation.id,
                            typing: false
                        }));
                    }
                    typingIndicator.style.display = 'none';
                }, 1500);
            }
        });

        // ===== EMOJI PICKER =====
        const emojiBtn = document.getElementById('emojiBtn');
        const emojiPicker = document.getElementById('emojiPicker');

        // Mở / đóng picker
        emojiBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            emojiPicker.style.display =
                emojiPicker.style.display === 'block' ? 'none' : 'block';
        });

        // Chặn click trong picker (để không đóng)
        emojiPicker.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        // Click emoji → thêm 1 emoji (KHÔNG GỬI)
        emojiPicker.querySelectorAll('.emoji').forEach(emoji => {
            emoji.addEventListener('click', (e) => {
                e.preventDefault();
                messageInput.value += emoji.textContent;
                messageInput.focus();
            });
        });

        // Chuyển tab emoji
        emojiPicker.querySelectorAll('.emoji-tab').forEach(tab => {
            tab.addEventListener('click', (e) => {
                e.preventDefault();

                emojiPicker.querySelectorAll('.emoji-tab')
                    .forEach(t => t.classList.remove('active'));
                emojiPicker.querySelectorAll('.emoji-content')
                    .forEach(c => c.classList.remove('active'));

                tab.classList.add('active');
                document.getElementById(tab.dataset.tab).classList.add('active');
            });
        });

        // Click ngoài → đóng picker
        document.addEventListener('click', () => {
            emojiPicker.style.display = 'none';
        });

         // Upload file button
            const attachBtn = document.getElementById('attachBtn');
            const fileInput = document.getElementById('fileInput');

            attachBtn.addEventListener('click', () => {
                fileInput.click();
            });

            fileInput.addEventListener('change', async (e) => {
                const file = e.target.files[0];
                if (!file) return;

                // Validate file
                const maxSize = 15 * 1024 * 1024; // 15MB
                const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'application/pdf'];

                if (file.size > maxSize) {
                    alert('File quá lớn! Tối đa 10MB');
                    return;
                }

                if (!allowedTypes.includes(file.type)) {
                    alert('Chỉ cho phép file ảnh (JPEG, PNG, GIF) hoặc PDF');
                    return;
                }

                // Upload file
                const uploadedData = await this.uploadFile(file);
                if (uploadedData) {
                    this.uploadedFiles.push({
                        file: file,
                        fileName: uploadedData.fileName,
                        originalName: file.name,
                        fileSize: uploadedData.fileSize,
                        contentType: uploadedData.contentType.startsWith('image/')
                                ? 'IMAGE'
                                : 'FILE'
                    });

                    this.showFilePreview(file, uploadedData);
                } else {
                    alert('Upload file thất bại!');
                }
            });

            // Send file button
            document.getElementById('sendFileBtn').addEventListener('click', () => {
                if (this.uploadedFiles.length > 0) {
                    const content = document.getElementById('messageInput').value;
                    this.sendMessageWithFile(content, this.uploadedFiles[0]);
                }
            });

            // Cancel file button
            document.getElementById('cancelFileBtn').addEventListener('click', () => {
                this.hideFilePreview();
            });

            // Close image view modal
            document.getElementById('imageViewModal').addEventListener('click', (e) => {
                if (e.target.id === 'imageViewModal') {
                    this.closeImageView();
                }
            });


    }

    getInitials(fullName) {
        return fullName
            .split(' ')
            .map(name => name[0])
            .join('')
            .toUpperCase()
            .substring(0, 2);
    }

    formatTime(timestamp) {
        const date = new Date(timestamp); // timestamp là ISO string

        if (isNaN(date.getTime())) {
            return 'Không rõ thời gian'; // fallback nếu lỗi
        }

        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).replace(',', '');
    }
    updateConversationItem(message) {
        const otherUserId =
            message.sender.id === this.currentUser.id
                ? message.receiver.id
                : message.sender.id;

        const convItem = document.querySelector(
            `.conversation-item[data-user-id="${otherUserId}"]`
        );

        if (!convItem) return;

        // Update last message
        const lastMessageEl = convItem.querySelector('.last-message');
        if (lastMessageEl) {
            lastMessageEl.textContent = message.content;
        }

        // Nếu không phải chat hiện tại → tăng unread
        if (!this.currentConversation || this.currentConversation.id !== otherUserId) {
            let unreadEl = convItem.querySelector('.unread-count');

            if (!unreadEl) {
                unreadEl = document.createElement('div');
                unreadEl.className = 'unread-count';
                unreadEl.textContent = '1';
                convItem.querySelector('.conversation-meta').prepend(unreadEl);
            } else {
                unreadEl.textContent = parseInt(unreadEl.textContent) + 1;
            }
        }

        // Đưa conversation lên đầu list
        const container = document.getElementById('conversationsList');
        container.prepend(convItem);
    }
    formatToastTime(timestamp) {
        const now = Date.now();
        const diffSec = Math.floor((now - new Date(timestamp).getTime()) / 1000);

        if (diffSec < 60) return 'Vừa xong';
        if (diffSec < 3600) return `${Math.floor(diffSec / 60)} phút`;
        if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}giờ`;
        return new Date(timestamp).toLocaleDateString('vi-VN');
    }
    startToastTimeUpdater() {
        if (this.toastTimeInterval) return;

        this.toastTimeInterval = setInterval(() => {
            this.activeToasts.forEach((toast) => {
                const timeEl = toast.querySelector('.toast-time');
                if (!timeEl || !toast.lastTime) return;

                timeEl.textContent = this.formatToastTime(toast.lastTime);
            });
        }, 60000); // mỗi 1 phút
    }

}
// Thêm vào window object
window.chatApp = null;
window.addEventListener('DOMContentLoaded', function () {
    window.chatApp = new ChatApp(); // Gán vào window để dùng ngoài
});



function saveEditedMessage() {
    chatApp.saveEditedMessage();
}
window.addEventListener('beforeunload', () => {
    try {
         if (window.chatApp?.stompClient?.connected) {
                window.chatApp.stompClient.disconnect();
            }
    } catch (e) {}
});
// Hàm logout chính
async function logout() {
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

        window.location.href = '/login-chat';
    }
}





