class ChatApp {
    constructor() {
        this.stompClient = null;
        this.currentUser = null;
        this.currentConversation = null;
        this.editingMessageId = null;
        this.isInCall = false;         // Đánh dấu đang trong cuộc gọi
        this.currentCallUserId = null; // Đánh dấu đang gọi với ai

        this.init();
    }

    async init() {
        // Luôn lấy lại accessToken mới nhất mỗi lần init
        this.accessToken = localStorage.getItem('accessToken');
        // Kiểm tra token
        // Đợi 200ms nếu token chưa sẵn sàng
        if (!this.accessToken) {
            setTimeout(() => {
                this.accessToken = localStorage.getItem('accessToken');
                if (!this.accessToken) {
                    window.location.href = '/login-chat';
                } else {
                    this.init();  // Gọi lại init
                }
            }, 200);
            return;
        }


        try {
            // Lấy thông tin user
            this.currentUser = JSON.parse(localStorage.getItem('userInfo'));
            document.getElementById('userFullName').textContent = this.currentUser.fullName;

            // Kết nối WebSocket
            await this.connectWebSocket();

            // Load conversations
            await this.loadConversations();

            // Setup event listeners
            this.setupEventListeners();
        } catch (error) {
            console.error('Init error:', error);
            // this.logout();
        }
        // if ("Notification" in window && Notification.permission !== "granted") {
        //     Notification.requestPermission();
        // }
        if (navigator.permissions) {
            navigator.permissions.query({name: 'notifications'}).then(function (result) {
                if (result.state === 'granted') {
                    console.log('Quyền thông báo đã được cấp.');
                } else if (result.state === 'denied') {
                    console.log('Quyền thông báo đã bị từ chối.');
                    alert('Thông báo đã bị chặn. Bạn có thể thay đổi quyền trong cài đặt trình duyệt.');
                } else {
                    console.log('Quyền thông báo chưa được xác định.');
                    Notification.requestPermission();
                }
            });
        }

        chatApp.showNotification("Thông báo thử", "Đây là nội dung thông báo");

    }

    // Thêm ở đây:


    connectWebSocket() {
        return new Promise((resolve, reject) => {
            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);
            // BẬT heartbeat (phải khớp với server)
            this.stompClient.heartbeat.outgoing = 15000;
            this.stompClient.heartbeat.incoming = 15000;

            this.stompClient.connect(
                {
                    'Authorization': `Bearer ${this.accessToken}`,
                    'userId': this.currentUser.id   // thêm userId ở header

                },
                (frame) => {
                    console.log('Connected: ' + frame);

                    // Subscribe to personal messages
                    this.stompClient.subscribe('/user/queue/messages', (message) => {
                        const messageData = JSON.parse(message.body);
                        this.handleNewMessage(messageData);
                    });

                    // Subscribe to message updates
                    this.stompClient.subscribe('/user/queue/message-updates', (message) => {
                        const messageData = JSON.parse(message.body);
                        this.handleMessageUpdate(messageData);
                    });
                    // Subscribe to typing status
                    this.stompClient.subscribe('/user/queue/typing', (message) => {
                        const data = JSON.parse(message.body);
                        const typingIndicator = document.getElementById('typingIndicator');

                        if (this.currentConversation && data.senderId === this.currentConversation.id) {
                            typingIndicator.style.display = data.typing ? 'block' : 'none';
                        }
                    });
                    // Subscribe to message deletes
                    this.stompClient.subscribe('/user/queue/message-deletes', (message) => {
                        const deleteInfo = JSON.parse(message.body);
                        this.handleMessageDelete(deleteInfo);
                    });
                    this.stompClient.subscribe('/user/queue/call', (message) => {
                        const signal = JSON.parse(message.body);
                        this.handleCallSignal(signal);
                    });
                    // subscribe presence
                    this.stompClient.subscribe("/topic/presence", (statusMsg) => {
                        const status = JSON.parse(statusMsg.body);
                        this.updateUserStatus(status.userId, status.online);
                    });


                    resolve();
                },
                (error) => {
                    console.error('WebSocket connection error:', error);
                    reject(error);
                }
            );
        });
    }

    initiateCall(receiverId) {
        if (this.isInCall) {
            alert("Bạn đang trong cuộc gọi. Vui lòng kết thúc trước khi gọi tiếp.");
            return;
        }

        const signal = {
            type: 'offer',
            senderId: this.currentUser.id,
            receiverId: receiverId,
            senderName: this.currentUser.fullName,
        };

        this.stompClient.send("/app/call.signal", {}, JSON.stringify(signal));

        // 👉 Đây là dòng bạn cần thêm để người gọi cũng khởi động kết nối và hiện nút
        this.startCall(receiverId, false);
    }


    handleCallSignal(signal) {
        console.log("🔔 Nhận tín hiệu:", signal);

        if (signal.type === 'offer' && signal.offer) {
            console.log("📥 Nhận SDP offer từ", signal.senderId);

            // Nếu chưa có peerConnection thì tạo mới
            if (!this.peerConnection) {
                const config = {iceServers: [{urls: 'stun:stun.l.google.com:19302'}]};
                this.peerConnection = new RTCPeerConnection(config);

                navigator.mediaDevices.getUserMedia({audio: true, video: false})
                    .then(stream => {
                        stream.getTracks().forEach(track => this.peerConnection.addTrack(track, stream));
                    });

                this.peerConnection.onicecandidate = event => {
                    if (event.candidate) {
                        this.stompClient.send("/app/call.candidate", {}, JSON.stringify({
                            type: 'candidate',
                            receiverId: signal.senderId,
                            candidate: event.candidate
                        }));
                    }
                };

                this.peerConnection.ontrack = event => {
                    const audio = document.getElementById("remoteAudio");
                    audio.srcObject = event.streams[0];
                    audio.play();
                };
            }

            this.peerConnection.setRemoteDescription(new RTCSessionDescription(signal.offer));
            this.peerConnection.createAnswer().then(answer => {
                this.peerConnection.setLocalDescription(answer);
                this.stompClient.send("/app/call.signal", {}, JSON.stringify({
                    type: 'answer',
                    senderId: this.currentUser.id,
                    receiverId: signal.senderId,
                    answer: answer,
                    senderName: this.currentUser.fullName
                }));
            });

        } else if (signal.type === 'offer' && !signal.offer) {
            if (this.isInCall) {
                // Gửi tín hiệu bận lại cho người gọi
                this.stompClient.send("/app/call.signal", {}, JSON.stringify({
                    type: 'busy',
                    senderId: this.currentUser.id,
                    receiverId: signal.senderId,
                    senderName: this.currentUser.fullName
                }));
                return;
            }

            // Hiển thị modal nhận cuộc gọi
            this.showIncomingCallModal(signal.senderId, signal.senderName);
        } else if (signal.type === 'answer' && signal.answer) {
            console.log("✅ Đã nhận answer từ người nhận");

            this.peerConnection.setRemoteDescription(new RTCSessionDescription(signal.answer));

        } else if (signal.type === 'candidate' && signal.candidate) {
            console.log("🧊 Nhận ICE candidate");

            this.peerConnection.addIceCandidate(new RTCIceCandidate(signal.candidate));
        } else if (signal.type === 'busy') {
            alert(`${signal.senderName} hiện đang bận trong một cuộc gọi khác.`);
        } else if (signal.type === 'end') {
            console.log('📞 Cuộc gọi kết thúc từ phía bên kia');

            this.endCall(false); // Dọn dẹp UI và peer
        }


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


    startCall(peerId, isReceiver) {
        if (this.isInCall) {
            console.warn('Đã trong cuộc gọi, không thể bắt đầu cuộc gọi mới');
            return;
        }

        this.isInCall = true;
        this.currentCallUserId = peerId;

        // 👉 Hiện nút kết thúc cuộc gọi cho cả 2 bên
        const endCallBtn = document.getElementById("endCallBtn");
        if (endCallBtn) {
            endCallBtn.style.display = "inline-block";
        }

        const config = {iceServers: [{urls: 'stun:stun.l.google.com:19302'}]};
        this.peerConnection = new RTCPeerConnection(config);

        navigator.mediaDevices.getUserMedia({audio: true, video: false})
            .then(stream => {
                stream.getTracks().forEach(track => this.peerConnection.addTrack(track, stream));
            });

        this.peerConnection.onicecandidate = event => {
            if (event.candidate) {
                this.stompClient.send("/app/call.candidate", {}, JSON.stringify({
                    type: 'candidate',
                    receiverId: peerId,
                    candidate: event.candidate
                }));
            }
        };

        this.peerConnection.ontrack = event => {
            const audio = document.getElementById("remoteAudio");
            audio.srcObject = event.streams[0];
            audio.play();
        };

        if (!isReceiver) {
            this.peerConnection.createOffer()
                .then(offer => {
                    this.peerConnection.setLocalDescription(offer);
                    this.stompClient.send("/app/call.signal", {}, JSON.stringify({
                        type: 'offer',
                        receiverId: peerId,
                        offer: offer,
                        senderId: this.currentUser.id,
                        senderName: this.currentUser.fullName
                    }));
                });
        }
    }

    showIncomingCallModal(senderId, senderName) {
        const modal = document.getElementById("incomingCallModal");
        const callerNameEl = document.getElementById("callerName");
        const acceptBtn = document.getElementById("acceptCallBtn");
        const rejectBtn = document.getElementById("rejectCallBtn");

        this.ringtoneAudio = new Audio('/audio/nhacgoi.mp3');
        this.ringtoneAudio.loop = true;
        this.ringtoneAudio.play().catch((err) => {
            console.warn('Ringtone play blocked (user interaction required):', err);
        });

        callerNameEl.textContent = senderName;
        modal.style.display = 'flex';

        // Gỡ listener cũ
        acceptBtn.onclick = null;
        rejectBtn.onclick = null;

        acceptBtn.onclick = () => {
            // ✅ TẮT CHUÔNG
            if (this.ringtoneAudio) {
                this.ringtoneAudio.pause();
                this.ringtoneAudio.currentTime = 0;
            }

            modal.style.display = 'none';
            this.isInCall = true;
            this.currentCallUserId = senderId;
            this.startCall(senderId, true);  // người nhận gọi
            const endCallBtn = document.getElementById("endCallBtn");
            if (endCallBtn) {
                endCallBtn.style.display = "inline-block";
            }

        };

        rejectBtn.onclick = () => {
            // ✅ TẮT CHUÔNG
            if (this.ringtoneAudio) {
                this.ringtoneAudio.pause();
                this.ringtoneAudio.currentTime = 0;
            }

            modal.style.display = 'none';
        };
    }


    async apiCall(url, options = {}) {
        const defaultOptions = {
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${this.accessToken}`
            }
        };

        const response = await fetch(url, {...defaultOptions, ...options});

        if (response.status === 401) {
            // this.logout();
            return;
        }

        return response.json();
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

    endCall(sendSignal = true) {
        // Gửi tín hiệu endCall tới peer TRƯỚC khi reset
        if (sendSignal && this.stompClient && this.currentCallUserId) {
            this.stompClient.send("/app/call.signal", {}, JSON.stringify({
                type: 'end',
                senderId: this.currentUser.id,
                receiverId: this.currentCallUserId,
            }));
        }

        if (this.peerConnection) {
            this.peerConnection.close();
            this.peerConnection = null;
        }

        this.isInCall = false;
        this.currentCallUserId = null;

        // Reset UI
        const audio = document.getElementById("remoteAudio");
        if (audio) {
            audio.srcObject = null;
        }

        if (this.ringtoneAudio) {
            this.ringtoneAudio.pause();
            this.ringtoneAudio.currentTime = 0;
        }

        const modal = document.getElementById("incomingCallModal");
        if (modal) {
            modal.style.display = "none";
        }

        const endCallBtn = document.getElementById("endCallBtn");
        if (endCallBtn) {
            endCallBtn.style.display = "none"; // ⬅️ Quan trọng: luôn ẩn nút endCallBtn
        }
    }


    renderConversations(conversations) {
        const container = document.getElementById('conversationsList');
        container.innerHTML = '';

        conversations.forEach(conversation => {
            const div = document.createElement('div');
            div.className = 'conversation-item';
            div.dataset.userId = conversation.otherUser.id;

            const initials = this.getInitials(conversation.otherUser.fullName);
            const lastMessageText = conversation.lastMessage ?
                conversation.lastMessage.content : 'Chưa có tin nhắn';
            const timestamp = conversation.lastMessage ?
                this.formatTime(conversation.lastMessage.createdAt) : '';

            div.innerHTML = `
                <div class="avatar" id="chatUser-Search">${initials}</div>
                <div class="conversation-info">
                    <div class="conversation-name">${conversation.otherUser.fullName}</div>
                    <div class="last-message">${lastMessageText}</div>
                </div>
                <div class="conversation-meta">
                    <div class="timestamp">${timestamp}</div>
                    ${conversation.unreadCount > 0 ?
                `<div class="unread-count">${conversation.unreadCount}</div>` : ''}
                    <button 
                        onclick="chatApp.initiateCall('${conversation.otherUser.id}')" 
                             class="btn btn-outline-secondary btn-sm call-button"   title="Gọi điện"
                          ${this.isInCall ? 'disabled' : ''}>
                        <i class="fas fa-phone"></i>
                    </button>

                </div>
            `;

            div.addEventListener('click', () => this.selectConversation(conversation.otherUser));
            container.appendChild(div);
        });
    }

    async selectConversation(user) {
        this.currentConversation = user;

        // Update UI
        document.querySelectorAll('.conversation-item').forEach(item => {
            item.classList.remove('active');
        });

        const element = document.querySelector(`[data-user-id="${user.id}"]`);
        if (element) {
            element.classList.add('active');
        } else {
            console.warn(`Không tìm thấy phần tử với data-user-id="${user.id}"`);
        }


        const emptyChat = document.getElementById('emptyChat');
        if (emptyChat) emptyChat.style.display = 'none';

        // document.getElementById('chatHeader').style.display = 'flex';
        const chatHeader = document.getElementById('chatHeader');
        if (chatHeader) chatHeader.style.display = 'flex';
        // document.getElementById('messageInputContainer').style.display = 'block';
        const inputContainer = document.getElementById('messageInputContainer');
        if (inputContainer) inputContainer.style.display = 'block';


        const chatUserNameEl = document.getElementById('chatUserName');
        if (chatUserNameEl) {
            chatUserNameEl.textContent = user.fullName;
        }


        const chatUserAvatarEl = document.getElementById('chatUserAvatar');
        if (chatUserAvatarEl) {
            chatUserAvatarEl.innerHTML = ''; // Xóa nội dung cũ

            if (user.avatar) {
                const img = document.createElement('img');
                img.src = 'http://localhost:8081/storage/user/' + user.avatar;
                img.alt = user.fullName;
                img.className = 'avatar-img';  // Thêm class để style ảnh đẹp

                chatUserAvatarEl.appendChild(img);
            } else {
                chatUserAvatarEl.textContent = this.getInitials(user.fullName);
            }
        }
        const chatUser = document.getElementById('chatUser-Search');
        if (chatUser) {
            chatUser.innerHTML = ''; // Xóa nội dung cũ
            if (user.avatar) {
                const img = document.createElement('img');
                img.src = 'http://localhost:8081/storage/user/' + user.avatar;
                img.alt = user.fullName;
                img.className = 'avatar-img';  // Thêm class để style ảnh đẹp
                chatUser.appendChild(img);
            } else {
                chatUser.textContent = this.getInitials(user.fullName);
            }
        }
        const chatUserStatusEl = document.getElementById('chatUserStatus');
        if (chatUserStatusEl) {
            chatUserStatusEl.textContent = "Đang kiểm tra...";
        }
        // ✅ gọi API check online
        try {
            const res = await this.apiCall(`/api/v1/users/${user.id}/status`);
            if (res.online) {
                chatUserStatusEl.textContent = "Online";
                chatUserStatusEl.className = "text-success";
            } else {
                chatUserStatusEl.textContent = "Offline";
                chatUserStatusEl.className = "text-muted";
            }
        } catch (err) {
            console.error("Check status error:", err);
            chatUserStatusEl.textContent = "Offline";
            chatUserStatusEl.className = "text-muted";
        }



        // Load messages
        await this.loadMessages(user.id);
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

    createMessageElement(message) {
        const div = document.createElement('div');
        div.className = `message ${message.sender.id === this.currentUser.id ? 'own' : ''}`;
        div.dataset.messageId = message.id;

        const time = this.formatTime(message.createdAt);
        const editedText = message.isEdited ? ' (đã sửa)' : '';

        div.innerHTML = `
            <div class="message-bubble">
                <div class="message-content">${message.content}</div>
                <div class="message-time">${time}${editedText}</div>
                ${message.sender.id === this.currentUser.id ? `
                    <div class="message-actions">
                        <button class="btn btn-sm btn-outline-secondary me-1"
                                onclick="chatApp.editMessage(${message.id}, '${message.content}')">
                            <i class="fas fa-edit"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger"
                                onclick="chatApp.deleteMessage(${message.id})">
                            <i class="fas fa-trash"></i>
                        </button>
                    </div>
                ` : ''}
            </div>
        `;

        return div;
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
                // Remove message from UI
                const messageElement = document.querySelector(`[data-message-id="${messageId}"]`);
                if (messageElement) {
                    messageElement.remove();
                }
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
            container.innerHTML = '<div class="text-center p-3 text-muted">Không tìm thấy người dùng</div>';
        } else {
            users.forEach(user => {
                const div = document.createElement('div');
                div.className = 'user-search-item';
                div.innerHTML = `
                    <div class="avatar me-3" >${this.getInitials(user.fullName)}</div>
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
        // Nếu không phải trong cuộc trò chuyện hiện tại => thông báo
        const isInCurrentChat = this.currentConversation &&
            (message.sender.id === this.currentConversation.id || message.receiver.id === this.currentConversation.id);

        if (!isInCurrentChat) {
            this.showNotification(message.sender.fullName, message.content);
        }

        // Thêm tin nhắn vào cuộc trò chuyện hiện tại
        if (isInCurrentChat) {
            const messageElement = this.createMessageElement(message);
            document.getElementById('chatMessages').appendChild(messageElement);

            const chatMessages = document.getElementById('chatMessages');
            chatMessages.scrollTop = chatMessages.scrollHeight;
        }

        this.loadConversations();
    }

    showNotification(title, body) {
        if (Notification.permission === 'granted') {
            new Notification(title, {
                body: body,
                icon: '/img/chat-icon.png'
            });

            // 🔊 Play sound
            const audio = new Audio('/audio/notification.mp3');
            audio.play();
        }
    }


    handleMessageUpdate(message) {
        const messageElement = document.querySelector(`[data-message-id="${message.id}"]`);
        if (messageElement) {
            const contentElement = messageElement.querySelector('.message-content');
            const timeElement = messageElement.querySelector('.message-time');

            contentElement.textContent = message.content;
            timeElement.textContent = this.formatTime(message.updatedAt) + ' (đã sửa)';
        }
    }

    handleMessageDelete(deleteInfo) {
        const messageElement = document.querySelector(`[data-message-id="${deleteInfo.messageId}"]`);
        if (messageElement) {
            const contentElement = messageElement.querySelector('.message-content');
            contentElement.textContent = deleteInfo.status || 'Tin nhắn đã bị xóa';

            // Optional: Thêm class để style khác đi nếu muốn
            messageElement.classList.add('deleted-message');

            // Nếu muốn ẩn các nút sửa/xóa sau khi xóa, bạn có thể làm như sau:
            const actions = messageElement.querySelector('.message-actions');
            if (actions) {
                actions.remove();
            }
        }
    }


    setupEventListeners() {
        const endCallBtn = document.getElementById("endCallBtn");
        if (endCallBtn) {
            endCallBtn.onclick = () => this.endCall();
        }

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
        // Nếu là string thì ép sang số
        let ts = Number(timestamp);

        // Nếu là epoch giây (10 số) thì nhân 1000 để thành mili giây
        if (ts < 1000000000000) {
            ts *= 1000;
        }

        const date = new Date(ts);

        return date.toLocaleString('vi-VN', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        }).replace(',', ''); // bỏ dấu phẩy mặc định
    }


    logout() {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('userInfo');
        window.location.href = '/login-chat';
    }

}

window.addEventListener('DOMContentLoaded', function () {
    window.chatApp = new ChatApp(); // Gán vào window để dùng ngoài
});

function logout() {
    chatApp.logout();
}

function saveEditedMessage() {
    chatApp.saveEditedMessage();
}
window.addEventListener('beforeunload', () => {
    try {
        if (this.stompClient && this.stompClient.connected) {
            // Gửi DISCONNECT đúng chuẩn STOMP
            this.stompClient.disconnect(() => {}, {});
        }
    } catch (e) {}
});




