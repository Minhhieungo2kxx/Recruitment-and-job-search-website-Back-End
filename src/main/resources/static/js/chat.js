class ChatApp {
    constructor() {
        this.stompClient = null;
        this.currentUser = null;
        this.currentConversation = null;
        this.editingMessageId = null;
        this.isInCall = false;         // Đánh dấu đang trong cuộc gọi
        this.currentCallUserId = null; // Đánh dấu đang gọi với ai


        // Thêm cho presence system
        this.userPresences = new Map();
        this.presenceUpdateInterval = null;
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

            // Khởi động cập nhật presence định kỳ
            this.startPresenceUpdates();
        } catch (error) {
            console.error('Init error:', error);
            // this.logout();
        }
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

                    // SUBSCRIBE PRESENCE - CẬP NHẬT MỚI
                    this.stompClient.subscribe("/topic/presence", (statusMsg) => {
                        const presence = JSON.parse(statusMsg.body);
                        this.handlePresenceUpdate(presence);
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
        // Clear interval cũ nếu có
        if (this.presenceUpdateInterval) {
            clearInterval(this.presenceUpdateInterval);
        }

        // Cập nhật mỗi phút cho offline users
        this.presenceUpdateInterval = setInterval(() => {
            this.userPresences.forEach((presence, userId) => {
                if (!presence.isOnline && presence.lastSeenAt) {
                    // Recalculate status text
                    presence.statusText = this.formatRelativeTime(presence.lastSeenAt);
                    this.updateUserStatusUI(presence);
                }
            });
        }, 50000); // 1 phút
    }

    // Format thời gian tương đối tiếng Việt
    formatRelativeTime(lastSeenAt) {
        if (!lastSeenAt) return 'Chưa từng truy cập';

        let lastSeen;

        // Nếu là số (epoch giây), chuyển thành milliseconds
        if (typeof lastSeenAt === 'number') {
            lastSeen = new Date(lastSeenAt * 1000); // ✅ Nhân 1000 ở đây
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

        // Lấy presence cho tất cả users trong conversations
        const userIds = conversations.map(c => c.otherUser.id);
        this.fetchMultipleUsersPresence(userIds);

        conversations.forEach(conversation => {
            const div = document.createElement('div');
            div.className = 'conversation-item';
            div.dataset.userId = conversation.otherUser.id;

            const initials = this.getInitials(conversation.otherUser.fullName);
            const lastMessageText = conversation.lastMessage ?
                conversation.lastMessage.content : 'Chưa có tin nhắn';


            div.innerHTML = `
                <div class="avatar" id="chatUser-Search">
                    ${initials}
                    <span class="avatar-status-indicator"></span>
                </div>
                <div class="conversation-info">
                    <div class="conversation-name">${conversation.otherUser.fullName}</div>
                     <div class="user-status-text"></div>
                    <div class="last-message">${lastMessageText}</div>
                     
                </div>
                <div class="conversation-meta">
                    
                    ${conversation.unreadCount > 0 ?
                `<div class="unread-count">${conversation.unreadCount}</div>` : ''}
                    <button 
                        onclick="chatApp.initiateCall('${conversation.otherUser.id}')" 
                        class="btn btn-outline-secondary btn-sm call-button" title="Gọi điện"
                        ${this.isInCall ? 'disabled' : ''}>
                        <i class="fas fa-phone"></i>
                    </button>
                </div>
            `;

            div.addEventListener('click', () => this.selectConversation(conversation.otherUser));
            container.appendChild(div);
        });
    }

    // CẬP NHẬT METHOD selectConversation
    async selectConversation(user) {
        this.currentConversation = user;

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

        // Load messages
        await this.loadMessages(user.id);
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
            this.logout();
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




