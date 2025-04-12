// 전역 변수
let currentUser = null;
let notifications = [];
let unreadNotificationCount = 0;
let darkMode = localStorage.getItem('darkMode') === 'true';

// 페이지 로드 시 실행
document.addEventListener('DOMContentLoaded', async function() {
    try {
        // 사이드바 로드
        await loadComponent('sidebar-container', '/components/sidebar.html');
        
        // 네비게이션 바 로드
        await loadComponent('navbar-container', '/components/navbar.html');
        
        // 현재 사용자 정보 로드
        await loadUserInfo();
        
        // 사이드바 토글 이벤트 설정
        setupSidebarToggle();
        
        // 현재 페이지에 해당하는 메뉴 활성화
        activateCurrentPageMenu();
        
        // 다크 모드 설정 적용
        applyTheme();
        
        // 알림 로드
        await loadNotifications();
        
        // WebSocket 연결 설정
        setupWebSocket();
        
        // 5분마다 알림 수 업데이트
        setInterval(updateNotificationCount, 300000);
        
        // 로그아웃 버튼 이벤트 설정
        const logoutButton = document.querySelector('a[onclick="logout(); return false;"]');
        if (logoutButton) {
            logoutButton.addEventListener('click', function(e) {
                e.preventDefault();
                logout();
            });
        }
        
    } catch (error) {
        console.error('페이지 초기화 오류:', error);
        if (error.status === 401) {
            window.location.href = '/login.html';
        }
    }
});

// 컴포넌트 로드
async function loadComponent(containerId, url) {
    try {
        const response = await fetch(url);
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
        const html = await response.text();
        document.getElementById(containerId).innerHTML = html;
    } catch (error) {
        console.error(`컴포넌트 로드 실패 (${url}):`, error);
        throw error;
    }
}

// 사용자 정보 로드
async function loadUserInfo() {
    try {
        const response = await fetch('/api/users/me');
        if (!response.ok) throw new Error('사용자 정보를 불러오는데 실패했습니다.');
        
        const user = await response.json();
        
        // 사용자명 표시
        const userNameElements = document.querySelectorAll('.user-name');
        userNameElements.forEach(el => {
            el.textContent = user.username === 'admin' ? '관리자' : user.name;
        });
        
        // 프로필 이미지 표시
        const userAvatarElements = document.querySelectorAll('.user-avatar');
        userAvatarElements.forEach(el => {
            el.src = user.avatarUrl || '/images/default-avatar.png';
        });
        
        // 알림 수 업데이트
        updateNotificationCount();
        
    } catch (error) {
        console.error('사용자 정보 로드 실패:', error);
    }
}

// 알림 수 업데이트
async function updateNotificationCount() {
    try {
        const response = await fetch('/api/notifications/unread-count');
        if (!response.ok) throw new Error('알림 수를 불러오는데 실패했습니다.');
        
        const { count } = await response.json();
        const badge = document.querySelector('#notificationDropdown .badge');
        
        if (badge) {
            badge.textContent = count;
            badge.style.display = count > 0 ? 'inline' : 'none';
        }
        
    } catch (error) {
        console.error('알림 수 업데이트 실패:', error);
    }
}

// 사이드바 토글 설정
function setupSidebarToggle() {
    const sidebarCollapse = document.getElementById('sidebarCollapse');
    const sidebar = document.getElementById('sidebar');
    
    if (sidebarCollapse && sidebar) {
        sidebarCollapse.addEventListener('click', function() {
            sidebar.classList.toggle('active');
            
            // 토글 상태 저장
            localStorage.setItem('sidebarState', sidebar.classList.contains('active') ? 'collapsed' : 'expanded');
        });
        
        // 저장된 상태 복원
        const sidebarState = localStorage.getItem('sidebarState');
        if (sidebarState === 'collapsed') {
            sidebar.classList.add('active');
        } else if (sidebarState === 'expanded') {
            sidebar.classList.remove('active');
        }
    }
}

// 현재 페이지 메뉴 활성화
function activateCurrentPageMenu() {
    const currentPath = window.location.pathname;
    const menuLinks = document.querySelectorAll('.nav-link');
    
    menuLinks.forEach(link => {
        if (link.getAttribute('href') === currentPath) {
            link.classList.add('active');
            
            // 부모 메뉴 확장
            const parentCollapse = link.closest('.collapse');
            if (parentCollapse) {
                parentCollapse.classList.add('show');
                const parentToggle = document.querySelector(`[href="#${parentCollapse.id}"]`);
                if (parentToggle) {
                    parentToggle.classList.add('active');
                }
            }
        }
    });
}

// 다크 모드 설정 적용
function applyTheme() {
    const settings = JSON.parse(localStorage.getItem('settings') || '{}');
    if (settings.darkMode) {
        document.body.classList.add('dark-mode');
    } else {
        document.body.classList.remove('dark-mode');
    }
}

// 테마 토글
function toggleTheme() {
    const settings = JSON.parse(localStorage.getItem('settings') || '{}');
    settings.darkMode = !settings.darkMode;
    localStorage.setItem('settings', JSON.stringify(settings));
    applyTheme();
}

// 로딩 오버레이 표시
function showLoading() {
    const overlay = document.createElement('div');
    overlay.className = 'loading-overlay';
    overlay.innerHTML = '<div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div>';
    document.body.appendChild(overlay);
}

// 로딩 오버레이 숨김
function hideLoading() {
    const overlay = document.querySelector('.loading-overlay');
    if (overlay) {
        overlay.remove();
    }
}

// 토스트 메시지 표시
function showToast(type, title, message) {
    const toastContainer = document.getElementById('toast-container');
    if (!toastContainer) return;
    
    const toast = document.createElement('div');
    toast.className = `toast align-items-center text-white bg-${type === 'error' ? 'danger' : 'success'} border-0`;
    toast.setAttribute('role', 'alert');
    toast.setAttribute('aria-live', 'assertive');
    toast.setAttribute('aria-atomic', 'true');
    
    toast.innerHTML = `
        <div class="d-flex">
            <div class="toast-body">
                <strong>${title}</strong><br>
                ${message}
            </div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
    
    toast.addEventListener('hidden.bs.toast', function() {
        toast.remove();
    });
}

// 로그아웃
async function logout() {
    try {
        showLoading();
        
        const response = await fetch('/api/auth/logout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        
        if (!response.ok) throw new Error('로그아웃에 실패했습니다.');
        
        // 로컬 스토리지 초기화
        localStorage.clear();
        
        // 세션 스토리지 초기화
        sessionStorage.clear();
        
        // 로그인 페이지로 이동
        window.location.href = '/login.html';
        
    } catch (error) {
        hideLoading();
        showToast('error', '로그아웃 실패', error.message);
    }
}

// 알림 로드
async function loadNotifications() {
    try {
        const response = await fetch('/api/notifications');
        if (!response.ok) throw new Error('알림을 불러오는데 실패했습니다.');
        
        const data = await response.json();
        notifications = data.notifications;
        unreadNotificationCount = data.unreadCount;
        updateNotificationBadge();
        renderNotifications();
    } catch (error) {
        console.error('알림 로드 오류:', error);
    }
}

// 알림 뱃지 업데이트
function updateNotificationBadge() {
    const badge = document.querySelector('.notification-badge');
    if (badge) {
        badge.textContent = unreadNotificationCount;
        badge.style.display = unreadNotificationCount > 0 ? 'block' : 'none';
    }
}

// 알림 렌더링
function renderNotifications() {
    const container = document.querySelector('.notification-list');
    if (!container) return;

    container.innerHTML = '';
    
    if (notifications.length === 0) {
        container.innerHTML = `
            <div class="text-center py-3">
                <i class="mdi mdi-bell-off text-muted" style="font-size: 24px;"></i>
                <p class="text-muted mb-0">새로운 알림이 없습니다.</p>
            </div>
        `;
        return;
    }

    notifications.forEach(notification => {
        const item = document.createElement('div');
        item.className = `notification-item ${notification.isRead ? '' : 'unread'}`;
        item.innerHTML = `
            <div class="d-flex align-items-center p-3">
                <div class="flex-shrink-0">
                    <i class="mdi ${getNotificationIcon(notification.type)} ${getNotificationColor(notification.type)}" style="font-size: 24px;"></i>
                </div>
                <div class="flex-grow-1 ms-3">
                    <p class="mb-1">${notification.message}</p>
                    <small class="text-muted">${formatDate(notification.createdAt)}</small>
                </div>
                ${notification.isRead ? '' : `
                    <div class="flex-shrink-0 ms-2">
                        <button class="btn btn-sm btn-link text-muted mark-as-read" data-id="${notification.id}">
                            <i class="mdi mdi-check"></i>
                        </button>
                    </div>
                `}
            </div>
        `;

        // 읽음 표시 이벤트
        const markAsReadBtn = item.querySelector('.mark-as-read');
        if (markAsReadBtn) {
            markAsReadBtn.addEventListener('click', () => markNotificationAsRead(notification.id));
        }

        container.appendChild(item);
    });
}

// 알림 읽음 표시
async function markNotificationAsRead(id) {
    try {
        const response = await fetch(`/api/notifications/${id}/read`, {
            method: 'POST'
        });
        if (!response.ok) throw new Error('알림 읽음 표시에 실패했습니다.');
        
        // 알림 상태 업데이트
        const notification = notifications.find(n => n.id === id);
        if (notification && !notification.isRead) {
            notification.isRead = true;
            unreadNotificationCount--;
            updateNotificationBadge();
            renderNotifications();
        }
    } catch (error) {
        console.error('알림 읽음 표시 오류:', error);
        showNotification('알림 읽음 표시에 실패했습니다.', 'error');
    }
}

// WebSocket 설정
function setupWebSocket() {
    const ws = new WebSocket(`ws://${window.location.host}/ws/notifications`);
    
    ws.onmessage = function(event) {
        const notification = JSON.parse(event.data);
        notifications.unshift(notification);
        unreadNotificationCount++;
        updateNotificationBadge();
        renderNotifications();
        showToast(notification.type, '알림', notification.message);
    };

    ws.onclose = function() {
        setTimeout(setupWebSocket, 5000); // 5초 후 재연결 시도
    };
}

// 알림 아이콘 가져오기
function getNotificationIcon(type) {
    const icons = {
        'PRINT': 'mdi-printer',
        'SCAN': 'mdi-scanner',
        'USER': 'mdi-account',
        'SYSTEM': 'mdi-cog',
        'ERROR': 'mdi-alert'
    };
    return icons[type] || 'mdi-bell';
}

// 알림 색상 가져오기
function getNotificationColor(type) {
    const colors = {
        'PRINT': 'text-primary',
        'SCAN': 'text-success',
        'USER': 'text-info',
        'SYSTEM': 'text-warning',
        'ERROR': 'text-danger'
    };
    return colors[type] || 'text-secondary';
}

// 알림 메시지 표시
function showNotification(message, type = 'info') {
    const icons = {
        'success': 'mdi-check-circle',
        'error': 'mdi-alert-circle',
        'warning': 'mdi-alert',
        'info': 'mdi-information'
    };
    showToast(type, '알림', message);
}

// 날짜 포맷
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// 에러 처리
function handleError(error) {
    console.error('Error:', error);
    showNotification(error.message || '오류가 발생했습니다.', 'danger');
} 