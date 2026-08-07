// Import tất cả và đặt tên là notificationApi
import * as notificationApi from './notificationApi.js';


const state = {

    accessToken: null,
    currentUser: null,
    // websocket
    socket: null,
    stompClient: null,

    notificationSubscription: null,
    // reconnect
    reconnectTimer: null,

    reconnectAttempts: 0,

    maxReconnectAttempts: 10,

    // connection status
    isConnecting: false,

    // refresh token
    isRefreshing: false,

    refreshPromise: null,

};
const notificationState = {
    currentPage: 1,
    pageSize: 10,
    totalPages: 1,
    hasMore: true,
    loading: false,
    notifications: []
};

function loadAuth() {
    state.accessToken = localStorage.getItem("accessToken");
    state.currentUser = JSON.parse(localStorage.getItem("userInfo"));
}

function getAccessToken() {
    return state.accessToken;

}

function getCurrentUser() {
    return state.currentUser;

}


async function init() {

    loadAuth();

    if (!state.accessToken || !state.currentUser) {

        console.log("No auth data");

        return false;
    }

    await connectWebSocket();

    return true;
}


function isAccessTokenExpired() {

    const token = state.accessToken;

    if (!token) {
        return true;
    }

    try {
        const payload = JSON.parse(atob(token.split(".")[1]));

        const now = Math.floor(Date.now() / 1000);

        return payload.exp <= now + 5;

    } catch (e) {
        console.error("Invalid JWT", e);
        return true;
    }

}

function subscribeWS() {
    if (!state.stompClient || !state.stompClient.connected) {
        console.error("STOMP not connected");
        return;
    }
    state.notificationSubscription =
        state.stompClient.subscribe("/user/queue/notification", (message) => {
            const data = JSON.parse(message.body);
            handleNotification(data.notification);
            updateNotificationBadge(data.unreadCount);

        });
    console.log("Subscribed notification channel");

}

function handleNotification(notification) {

    if (!notification) return;

    // cập nhật badge
    // updateNotificationBadge(notification.unreadCount);

    const list = document.getElementById("notificationList");
    const empty = document.getElementById("emptyNotification");

    if (!list) return;

    if (empty) {
        empty.style.display = "none";
    }

    // tránh trùng nếu notification đã tồn tại
    const existed = notificationState.notifications.find(
        n => n.id === notification.id
    );

    if (existed) return;

    // lưu vào state
    notificationState.notifications.unshift(notification);

    // thêm lên đầu giao diện
    list.prepend(createNotificationElement(notification));

}

function createNotificationElement(notification) {
    const item = document.createElement("div");
    item.className = `
    notification-item
    ${notification.read ? "" : "unread"}
    ${notification.pinned ? "pinned" : ""}`.trim();

    item.dataset.id = notification.id;


    //---------------- Header ----------------

    const header = document.createElement("div");
    header.className = "notification-header";

    const title = document.createElement("div");
    title.className = "title";
    title.textContent = notification.title || "Thông báo";


    const pinIcon = document.createElement("span");
    pinIcon.className = "pin-icon";
    pinIcon.innerHTML = "📌";
    pinIcon.style.display =
        notification.pinned ? "inline" : "none";
    title.prepend(pinIcon);

    // nút ba chấm
    const moreBtn = document.createElement("button");
    moreBtn.className = "notification-more";
    moreBtn.innerHTML = '<i class="fa-solid fa-ellipsis-vertical"></i>';

    // menu
    const menu = document.createElement("div");
    menu.className = "notification-menu";

    const pinItem = document.createElement("button");

    const unreadItem = document.createElement("button");

    const deleteItem = document.createElement("button");


    function updateMenuText() {

        if (notification.pinned) {

            pinItem.innerHTML =
                '<i class="fa-solid fa-thumbtack"></i> Bỏ ghim thông báo';

        } else {

            pinItem.innerHTML =
                '<i class="fa-solid fa-thumbtack"></i> Ghim thông báo';

        }


        if (notification.read) {

            unreadItem.innerHTML =
                '<i class="fa-solid fa-envelope"></i> Đánh dấu chưa đọc';

        } else {

            unreadItem.innerHTML =
                '<i class="fa-solid fa-envelope-open"></i> Đánh dấu đã đọc';

        }


        deleteItem.innerHTML =
            '<i class="fa-solid fa-trash"></i> Xóa thông báo';

    }

    updateMenuText();
    menu.append(pinItem, unreadItem, deleteItem);


    const menuWrapper = document.createElement("div");
    menuWrapper.className = "menu-wrapper";

    menuWrapper.append(moreBtn, menu);

    header.append(title, menuWrapper);

    //---------------- Body ----------------

    const content = document.createElement("div");
    content.className = "content";
    content.textContent = notification.content || "";

    const time = document.createElement("small");
    time.className = "time";

    time.textContent = notification.createdAt
        ? new Date(notification.createdAt).toLocaleString()
        : "Vừa xong";

    item.append(header, content, time);

    //---------------- Events ----------------

    moreBtn.addEventListener("click", function (e) {

        e.stopPropagation();

        document.querySelectorAll(".notification-menu.show")
            .forEach(m => {

                if (m !== menu)
                    m.classList.remove("show");

            });

        menu.classList.toggle("show");

    });

    deleteItem.addEventListener("click", async function (e) {

        e.stopPropagation();

        menu.classList.remove("show");

        await deleteNotification(notification.id, item);

    });
    pinItem.addEventListener(
        "click",
        async e => {

            e.stopPropagation();
            try {
                // Gọi API thông qua hàm vừa tách
                await notificationApi.togglePinNotification(notification.id, state.accessToken);
                // Cập nhật trạng thái giao diện
                notification.pinned = !notification.pinned;

                item.classList.toggle(
                    "pinned",
                    notification.pinned
                );
                pinIcon.style.display = notification.pinned ? "inline" : "none";
                updateMenuText();

            } catch (error) {
                console.error("Lỗi khi ghim thông báo:", error);
                // Có thể thêm thông báo lỗi cho người dùng ở đây (ví dụ: Toast, Alert)
            }


        });
    unreadItem.addEventListener(
        "click",
        async e => {

            e.stopPropagation();

            try {
                // Gọi API thông qua hàm vừa tách
                await notificationApi.toggleReadNotification(notification.id, state.accessToken);
                // Cập nhật trạng thái giao diện
                notification.read = !notification.read;

                item.classList.toggle(
                    "unread",
                    !notification.read
                );

                updateMenuText();

                loadUnreadCount();

            } catch (error) {
                console.error("Lỗi khi thay đổi trạng thái đọc:", error);
            }

        });


    return item;
}


async function refreshAccessToken() {
    if (state.isRefreshing && state.refreshPromise) {
        return state.refreshPromise;
    }
    state.isRefreshing = true;
    state.refreshPromise = (async () => {
        try {

            const result = await notificationApi.refreshTokenApi();

            if (result.statusCode !== 200 || !result.data?.access_token) {
                return false;
            }
            // update token
            state.accessToken = result.data.access_token;
            localStorage.setItem("accessToken", state.accessToken);
            // update user
            if (result.data.user) {
                state.currentUser = result.data.user;
                localStorage.setItem("userInfo", JSON.stringify(state.currentUser));
            }
            console.log("Refresh token thành công");

            return true;
        } catch (err) {
            console.error("Refresh token lỗi:", err);
            return false;
        } finally {
            state.isRefreshing = false;
            state.refreshPromise = null;
        }
    })();
    return state.refreshPromise;

}

async function connectWebSocket() {
    if (state.isConnecting) {
        console.log(
            "WebSocket connecting..."
        );
        return;
    }

    if (state.stompClient && state.stompClient.connected
    ) {
        console.log("WebSocket already connected");
        return;
    }
    state.isConnecting = true;

    try {
        if (isAccessTokenExpired()) {
            console.log("Access token expired, refreshing...");
            const ok = await refreshAccessToken();
            if (!ok) {
                console.log("Refresh failed");
                return;
            }
        }
        state.socket = new SockJS("/ws");
        state.stompClient = Stomp.over(state.socket);

        state.stompClient.heartbeat.outgoing = 15000;
        state.stompClient.heartbeat.incoming = 15000;

        state.stompClient.connect(
            {
                Authorization: `Bearer ${state.accessToken}`
            },
            () => {

                console.log("===== WS CONNECTED =====");
                state.isConnecting = false;
                state.reconnectAttempts = 0;

                subscribeWS();
                state.socket.onclose =
                    () => {
                        console.log("Socket closed");
                        reconnect();

                    };
            },
            (error) => {
                console.error("STOMP ERROR", error
                );

                state.isConnecting = false;
            }
        );
    } catch (error) {
        console.error("WebSocket exception", error);
        state.isConnecting = false;
    }
}

function reconnect() {
    if (state.reconnectAttempts >= state.maxReconnectAttempts) {
        console.error("Reconnect failed");
        return;

    }

    state.reconnectAttempts++;

    clearTimeout(state.reconnectTimer);

    state.reconnectTimer =
        setTimeout(() => {
            connectWebSocket();
        }, 2000);

}


function disconnectWebSocket() {

    clearTimeout(state.reconnectTimer);

    state.isConnecting = false;
    state.reconnectAttempts = 0;

    if (state.notificationSubscription) {
        state.notificationSubscription.unsubscribe();
        state.notificationSubscription = null;
    }

    if (state.stompClient) {
        try {
            state.stompClient.disconnect();
        } catch (e) {
        }
    }

    state.stompClient = null;
    state.socket = null;

}


window.addEventListener(
    "beforeunload",
    () => {
        disconnectWebSocket();
    }
);

function renderNotifications(notifications) {

    const list = document.getElementById("notificationList");

    if (!list) return;

    notifications.forEach(notification => {

        const existed = notificationState.notifications.find(
            n => n.id === notification.id
        );

        if (existed) return;

        notificationState.notifications.push(notification);

        list.appendChild(createNotificationElement(notification));

    });

}

async function loadMoreNotifications() {

    if (!notificationState.hasMore) return;

    await getNotifications(notificationState.currentPage + 1);

}

function toggleLoadMoreButton() {
    const btn = document.getElementById("loadMoreBtn");
    if (!btn) return;

    btn.style.display = notificationState.hasMore
        ? "block"
        : "none";
}

async function initNotifications() {


    notificationState.currentPage = 1;
    notificationState.notifications = [];
    notificationState.hasMore = true;

    document.getElementById("notificationList").innerHTML = "";

    await getNotifications(1);

}

async function getNotifications(page = 1) {

    if (notificationState.loading) return;

    notificationState.loading = true;

    try {

        const json = await notificationApi.fetchNotificationsApi(page, notificationState.pageSize, state.accessToken);
        notificationState.currentPage = json.data.meta.current;
        notificationState.totalPages = json.data.meta.totalPages;
        notificationState.hasMore = notificationState.currentPage < notificationState.totalPages;
        renderNotifications(json.data.result);
        toggleLoadMoreButton();
        checkEmptyNotification();

    } finally {

        notificationState.loading = false;

    }

}

async function loadUnreadCount() {
    try {
        const json = await notificationApi.fetchUnreadCountApi(state.accessToken);

        checkEmptyNotification();
        updateNotificationBadge(json.data);


    } catch (error) {
        // Xử lý lỗi nếu cần thiết
    }

}

function updateNotificationBadge(count) {

    const badge = document.getElementById("notificationCount");

    if (!badge) return;
    if (count <= 0) {

        badge.style.display = "none";

        badge.textContent = "";
    } else {

        badge.style.display = "flex";

        badge.textContent = count > 99 ? "99+" : count;

    }
}

async function markAllNotificationRead() {
    try {
        await notificationApi.markAllAsReadApi(state.accessToken);

        updateNotificationBadge(0);
        document.querySelectorAll(".notification-item.unread")
            .forEach(item => item.classList.remove("unread"));
        notificationState.notifications.forEach(n => {
            n.read = true;
        });
    } catch (err) {
        console.error(err);

    }
}

async function deleteAllNotification() {

    try {

        await notificationApi.clearAllNotificationsApi(state.accessToken);

        notificationState.notifications =
            notificationState.notifications.filter(n => !n.read);
        document
            .querySelectorAll(".notification-item")
            .forEach(item => {

                const id = Number(item.dataset.id);

                const notification =
                    notificationState.notifications.find(n => n.id === id);

                if (!notification) {

                    item.remove();

                }
            });

        checkEmptyNotification();

    } catch (err) {

        console.error(err);

    }

}


document.addEventListener("DOMContentLoaded", function () {
    const bell = document.getElementById("notificationIcon");
    const dropdown = document.getElementById("notificationDropdown");
    const wrapper = document.querySelector(".notification-wrapper");

    if (!bell || !dropdown || !wrapper) return;

    // 1. Click vào icon chuông để mở/đóng dropdown
    bell.addEventListener("click", async function (e) {
        e.stopPropagation(); // Ngăn sự kiện nổi bọt lên document
        const opening = !dropdown.classList.contains("show");
        dropdown.classList.toggle("show");
        checkEmptyNotification();


    });

    // 2. Chặn sự kiện click bên trong wrapper để không bị đóng nhầm
    wrapper.addEventListener("click", function (e) {
        e.stopPropagation();
    });

    // 3. Chỉ đóng dropdown khi click hoàn toàn ra ngoài vùng .notification-wrapper
    document.addEventListener("click", function () {
        dropdown.classList.remove("show");
    });

    // 4. Ẩn các menu nhỏ (3 chấm) khi click ra ngoài
    document.addEventListener("click", function () {
        document.querySelectorAll(".notification-menu")
            .forEach(menu => menu.classList.remove("show"));
    });

    // Các sự kiện nút bấm
    const loadMoreBtn = document.getElementById("loadMoreBtn");
    if (loadMoreBtn) {
        loadMoreBtn.addEventListener("click", loadMoreNotifications);
    }

    const deleteAllBtn = document.getElementById("deleteAllBtn");
    if (deleteAllBtn) {
        deleteAllBtn.addEventListener("click", deleteAllNotification);
    }
    const markAllReadBtn = document.getElementById("markAllReadBtn");
    markAllReadBtn.addEventListener("click", async () => {
        const hasUnread = notificationState.notifications.some(n => !n.read);
        if (!hasUnread) return;

        markAllReadBtn.disabled = true;

        try {
            await markAllNotificationRead();
        } finally {
            markAllReadBtn.disabled = false;
        }

    });
});

async function deleteNotification(id, element) {

    try {
        await notificationApi.deleteNotificationByIdApi(id, state.accessToken);
        element.remove();
        notificationState.notifications = notificationState.notifications.filter(n => n.id !== id);

        checkEmptyNotification();

    } catch (err) {

        console.error(err);

    }
}

function checkEmptyNotification() {

    const list = document.getElementById("notificationList");
    const empty = document.getElementById("emptyNotification");

    const markAllReadBtn = document.getElementById("markAllReadBtn");
    const deleteAllBtn = document.getElementById("deleteAllBtn");
    const loadMoreBtn = document.getElementById("loadMoreBtn");

    if (notificationState.notifications.length === 0) {

        // Hiện thông báo rỗng
        empty.style.display = "block";
        list.style.display = "none";

        // Ẩn các nút thao tác
        markAllReadBtn.style.display = "none";
        deleteAllBtn.style.display = "none";
        loadMoreBtn.style.display = "none";

    } else {

        // Có thông báo
        empty.style.display = "none";
        list.style.display = "block";

        // Hiện các nút thao tác
        markAllReadBtn.style.display = "inline-flex";
        deleteAllBtn.style.display = "inline-flex";
        loadMoreBtn.style.display = "block";
    }
}

async function autoStartWebSocket() {
    loadAuth();

    if (state.accessToken && state.currentUser) {
        console.log("Authenticated user -> connect websocket");

        await connectWebSocket();
        await loadUnreadCount();
        // Load thông báo đầu tiên
        await initNotifications();

    } else {
        console.log("No authentication -> skip websocket");
    }
}

autoStartWebSocket();