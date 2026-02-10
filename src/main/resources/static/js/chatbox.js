// Biến global
let isWaitingForResponse = false;
const chatBody = document.getElementById("chat-box-body");
const chatInput = document.getElementById("chat-input");
const chatSend = document.getElementById("chat-send");
let rateLimitTimeout = null;
let rateLimitUntil = null;


function checkLoginStatus() {
    const token = getAccessToken();

    if (!token) {
        chatInput.disabled = true;
        chatSend.disabled = true;
        chatInput.placeholder = "Vui lòng đăng nhập để sử dụng chức năng này";

        if (!document.querySelector('.login-required-message')) {
            const noticeDiv = document.createElement("div");
            noticeDiv.className = "message ai-message login-required-message";
            noticeDiv.innerHTML = `
                🔒 <strong>Bạn chưa đăng nhập</strong><br/>
                Vui lòng <em>đăng nhập</em> để sử dụng chức năng tư vấn nghề nghiệp nhé!
            `;
            chatBody.appendChild(noticeDiv);
            scrollToBottom();
        }
        return false;
    }

    chatInput.disabled = false;
    chatSend.disabled = false;
    chatInput.placeholder = "Hỏi tôi về sự nghiệp...";
    return true;
}


function getAccessToken() {
    return localStorage.getItem('accessToken');
}

function getAuthHeaders() {
    const token = getAccessToken();
    return {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${token}`
    };
}


// Toggle chat box
function toggleChatBox() {
    const chatBox = document.getElementById('chat-box');
    if (chatBox.style.display === 'none' || chatBox.style.opacity === '0') {
        chatBox.style.display = 'flex';
        setTimeout(() => {
            chatBox.style.opacity = '1';
            chatBox.style.transform = 'translateY(0)';
        }, 10);
    } else {
        chatBox.style.opacity = '0';
        chatBox.style.transform = 'translateY(20px)';
        setTimeout(() => {
            chatBox.style.display = 'none';
        }, 300);
    }
}

async function sendMessage() {
    if (!checkLoginStatus()) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) {
        displayAIMessage("⏳ Bạn đang bị giới hạn truy cập. Vui lòng chờ trong giây lát.");
        return;
    }
    const message = chatInput.value.trim();
    if (!message || isWaitingForResponse) return;

    displayUserMessage(message);
    chatInput.value = "";
    setLoadingState(true);
    showTypingIndicator();
    try {
        const res = await AuthService.apiCall("/api/v1/chat/send", {
            method: "POST",
            body: JSON.stringify({ message })
        });

        hideTypingIndicator();
        if (res.httpStatus === 429) {
            handleRateLimit(res);
            return;
        }
        if (res.statusCode === 200 && res.data) {
            displayAIMessage(res.data);
        } else {
            displayAIMessage(res?.message || "Xin lỗi, đã có lỗi xảy ra.");
        }

    } catch (err) {
        hideTypingIndicator();
        displayAIMessage("Không thể kết nối đến server.");
    } finally {
        if (!rateLimitUntil || Date.now() >= rateLimitUntil) {
            setLoadingState(false);
        }
    }



}


// Hiển thị tin nhắn của user
function displayUserMessage(message) {
    let text = '';
    let timestamp = null;

    if (!message) {
        text = "AI chưa trả lời";
    } else if (typeof message === 'string') {
        text = message;
    } else if (typeof message === 'object') {
        text = message.message;
        timestamp = message.timestamp || null;
    }
    const messageDiv = document.createElement("div");
    messageDiv.className = "message user-message";
    messageDiv.innerHTML = `
        <div>${escapeHtml(text)}</div>
        <div class="timestamp">${formatTimestamp(timestamp)}</div>
    `;
    chatBody.appendChild(messageDiv);
    scrollToBottom();
}

function displayAIMessage(message) {
    let text = '';
    let timestamp = null;

    if (!message) {
        text = "AI chưa trả lời";
    } else if (typeof message === 'string') {
        text = message;
    } else if (typeof message === 'object') {
        text = message.response || "AI chưa trả lời";
        timestamp = message.timestamp || null;
    }

    const messageDiv = document.createElement("div");
    messageDiv.className = "message ai-message";
    messageDiv.innerHTML = `
        <div>${formatAIMessage(text)}</div>
        <div class="timestamp">${formatTimestamp(timestamp)}</div>
    `;
    chatBody.appendChild(messageDiv);
    scrollToBottom();
}

// Hiển thị tin nhắn từ AI (hỗ trợ object hoặc string)

function formatAIMessage(rawMessage) {
    if (!rawMessage) return '';

    // Escape HTML để tránh XSS
    let escaped = escapeHtml(rawMessage);

    // In đậm: *text* → <b>text</b>
    escaped = escaped.replace(/\*(.*?)\*/g, '<b>$1</b>');

    // In nghiêng: _text_ → <i>text</i>
    escaped = escaped.replace(/_(.*?)_/g, '<i>$1</i>');

    // Code inline: `code` → <code>code</code>
    escaped = escaped.replace(/`(.*?)`/g, '<code>$1</code>');

    // Tách dòng
    const lines = escaped.split('\n');

    let result = '';
    let inList = false;

    for (let line of lines) {
        line = line.trim();

        if (line.startsWith('- ')) {
            // Mở <ul> nếu chưa mở
            if (!inList) {
                result += '<ul>';
                inList = true;
            }
            result += `<li>${line.slice(2)}</li>`;
        } else {
            // Đóng <ul> nếu đang mở
            if (inList) {
                result += '</ul>';
                inList = false;
            }
            // Dòng bình thường: nếu rỗng thì <br>, không thì <div>
            result += line ? `<div>${line}</div>` : '<br>';
        }
    }

    // Nếu còn list mở cuối cùng thì đóng
    if (inList) result += '</ul>';

    return result;
}


// Escape HTML để tránh XSS
function escapeHtml(text = '') {
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}
// Hiển thị typing indicator
function showTypingIndicator() {
    const existingIndicator = document.querySelector('.typing-indicator');
    if (existingIndicator) return;

    const typingDiv = document.createElement("div");
    typingDiv.className = "typing-indicator";
    typingDiv.innerHTML = 'AI đang trả lời<span class="typing-dots"></span>';
    chatBody.appendChild(typingDiv);
    scrollToBottom();
}

// Ẩn typing indicator
function hideTypingIndicator() {
    const typingIndicator = document.querySelector('.typing-indicator');
    if (typingIndicator) {
        typingIndicator.remove();
    }
}

// Set loading state
function setLoadingState(loading) {
    isWaitingForResponse = loading;
    chatSend.disabled = loading;
    chatInput.disabled = loading;
    chatSend.innerHTML = loading ? '<span class="spinner"></span>Đang gửi...' : 'Gửi';
}

// Scroll to bottom
function scrollToBottom() {
    chatBody.scrollTop = chatBody.scrollHeight;
}

// Load chat history
async function loadChatHistory() {
    if (!localStorage.getItem("accessToken")) return;
    if (rateLimitUntil && Date.now() < rateLimitUntil) {
        displayAIMessage("⏳ Bạn đang bị giới hạn truy cập. Vui lòng chờ trong giây lát.");
        return;
    }

    try {
        const res = await AuthService.apiCall("/api/v1/chat/history", {
            method: "GET"
        });

        if (res.httpStatus === 429) {
            handleRateLimit(res);
            return;
        }
        if (res.statusCode !== 200 || !res.data) return;

        if (!res.data || res.data.length === 0) {
            renderDefaultSystemMessage();
            return;
        }

        chatBody.innerHTML = '';
        res.data.forEach(item => {
            if (item.message) displayUserMessage(item);
            if (item.response) displayAIMessage(item);
        });

    } catch (err) {
        console.error("Load history error", err);
    }
}

// Clear chat history
async function clearChatHistory() {
    if (!confirm("Bạn có chắc chắn muốn xóa toàn bộ lịch sử chat?")) return;

    try {
        const res = await AuthService.apiCall("/api/v1/chat/history/clear", {
            method: "DELETE"
        });

        if (res?.statusCode === 200) {
            renderDefaultSystemMessage();
        } else {
            alert("Không thể xóa lịch sử chat.");
        }
    } catch (err) {
        console.error(err);
        alert("Có lỗi khi xóa lịch sử chat.");
    }
}


// Handle Enter key
function handleKey(event) {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
}

function parseTimestamp(ts) {
    if (!ts) return new Date();

    if (ts instanceof Date) return ts;

    if (typeof ts === 'number') {
        const d = new Date(ts);
        return isNaN(d) ? new Date() : d;
    }

    if (typeof ts === 'string') {
        // Bỏ microseconds nếu có
        let clean = ts.split('.')[0]; // "2026-01-06 17:42:49"
        // Tách từng phần
        const match = clean.match(/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/);
        if (match) {
            const [_, year, month, day, hour, minute, second] = match;
            return new Date(
                Number(year),
                Number(month) - 1, // JS months 0-11
                Number(day),
                Number(hour),
                Number(minute),
                Number(second)
            );
        }

        // fallback nếu không khớp
        const d = new Date(clean.replace(' ', 'T'));
        return isNaN(d) ? new Date() : d;
    }

    // fallback mọi trường hợp khác
    return new Date();
}

function formatTimestamp(ts) {
    const now = parseTimestamp(ts);

    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');

    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    return `${day}-${month}-${year} ${hours}:${minutes}:${seconds}`;
}

function renderDefaultSystemMessage() {
    chatBody.innerHTML = '';

    const systemMessage = document.createElement("div");
    systemMessage.className = "system-message";
    systemMessage.innerHTML = `
        👋 <strong>Chào bạn!</strong><br/>
        Tôi là trợ lý tư vấn nghề nghiệp.<br/>
        Bạn có thể hỏi tôi về:
        <ul>
            <li>🧭 Định hướng lộ trình phát triển sự nghiệp</li>
            <li>🔍 Gợi ý công việc phù hợp với năng lực</li>
            <li>Kỹ năng cần có cho từng vị trí</li>
            <li>📊 Phân tích xu hướng ngành & thị trường</li>
        </ul>
    `;

    chatBody.appendChild(systemMessage);
}
const AuthService = {
    accessToken: localStorage.getItem("accessToken"),
    currentUser: null,

    async apiCall(url, options = {}) {
        let accessToken = this.accessToken || localStorage.getItem("accessToken");
        //  GLOBAL RATE LIMIT BLOCK
        if (rateLimitUntil && Date.now() < rateLimitUntil) {
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

        // Refresh token
        if (response.status === 401 || response.status === 403) {
            const refreshed = await this.refreshAccessToken();
            if (!refreshed) throw new Error("Unauthorized");

            finalOptions.headers.Authorization = `Bearer ${this.accessToken}`;
            response = await fetch(url, finalOptions);
        }

        //  BẮT RATE LIMIT TẠI ĐÂY
        if (response.status === 429) {
            const retryAfter =
                Number(response.headers.get("Retry-After")) || 300;

            return {
                httpStatus: 429,
                statusCode: 429,
                retryAfter
            };
        }

        let body = null;
        try {
            body = await response.json();
        } catch {}

        return {
            httpStatus: response.status,
            ...body
        };
    },

    async refreshAccessToken() {
        try {
            const response = await fetch("/api/v1/auth/refresh", {
                method: "POST",
                credentials: "include", //  BẮT BUỘC
                headers: {"Content-Type": "application/json"}
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
    }
};
function handleRateLimit(errorResponse) {
    //  ĐÃ BỊ KHÓA RỒI → KHÔNG LÀM GÌ NỮA
    if (rateLimitUntil && Date.now() < rateLimitUntil) return;

    chatInput.disabled = true;
    chatSend.disabled = true;

    const retryAfterSeconds = errorResponse?.retryAfter || 300;
    rateLimitUntil = Date.now() + retryAfterSeconds * 1000;

    displayAIMessage(
        `🚫Bạn đã gửi quá nhiều yêu cầu
        Hệ thống tạm thời khóa trong ${Math.ceil(retryAfterSeconds / 60)} phút.
        ⏳ Vui lòng chờ để sử dụng lại.`
    );

    startRateLimitCountdown();
}

function startRateLimitCountdown() {
    if (rateLimitTimeout) clearInterval(rateLimitTimeout);

    rateLimitTimeout = setInterval(() => {
        const remaining = rateLimitUntil - Date.now();

        if (remaining <= 0) {
            clearInterval(rateLimitTimeout);
            rateLimitTimeout = null;
            rateLimitUntil = null;

            chatInput.disabled = false;
            chatSend.disabled = false;
            chatInput.placeholder = "Bạn có thể tiếp tục trò chuyện 👋";

            displayAIMessage("✅Bạn đã có thể sử dụng lại chat.");
            return;
        }
        const seconds = Math.ceil(remaining / 1000);
        chatSend.innerHTML = `Gửi`;
    }, 1000);
}



// DOM ready
document.addEventListener('DOMContentLoaded', function () {
    const chatBox = document.getElementById("chat-box");
    chatBox.style.display = 'flex';

    setTimeout(() => {
        chatBox.style.opacity = '1';
        chatBox.style.transform = 'translateY(0)';
        chatInput?.focus();
        checkLoginStatus(); //  thêm dòng này
        // if (getAccessToken()) {
        //     loadChatHistory();
        // }
    }, 50);
    let historyLoaded = false;
    const observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (mutation) {
            if (
                mutation.type === 'attributes' &&
                mutation.attributeName === 'style' &&
                chatBox.style.display === 'flex' &&
                !historyLoaded &&
                !(rateLimitUntil && Date.now() < rateLimitUntil)
            ) {
                historyLoaded = true;
                loadChatHistory();
            }
        });
    });

    observer.observe(chatBox, {
        attributes: true,
        attributeFilter: ['style']
    });
});

// Handle connection errors
window.addEventListener('online', () => console.log('Connection restored'));
window.addEventListener('offline', () => {
    console.log('Connection lost');
    displayAIMessage("Mất kết nối internet. Vui lòng kiểm tra kết nối và thử lại.");
});

