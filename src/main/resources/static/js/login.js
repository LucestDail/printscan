// 더미 사용자 데이터
const dummyUsers = [
    { username: 'admin', password: 'admin1234', role: 'ADMIN' },
    { username: 'user1', password: 'user1234', role: 'USER' },
    { username: 'manager', password: 'manager1234', role: 'MANAGER' }
];

// 페이지 로드 시 로그인 상태 체크
document.addEventListener('DOMContentLoaded', function() {
    // 이미 로그인된 상태인지 확인
    const user = JSON.parse(localStorage.getItem('user')) || JSON.parse(sessionStorage.getItem('user'));
    if (user) {
        window.location.href = 'dashboard.html';
        return;
    }

    const loginForm = document.getElementById('loginForm');
    
    loginForm.addEventListener('submit', function(e) {
        e.preventDefault();
        
        const username = document.getElementById('username').value;
        const password = document.getElementById('password');
        const rememberMe = document.getElementById('rememberMe').checked;
        
        // 더미 데이터로 로그인 검증
        const user = dummyUsers.find(u => u.username === username && u.password === password.value);
        
        if (user) {
            // 로그인 성공
            if (rememberMe) {
                // 로그인 정보 저장 (실제 구현에서는 보안을 고려해야 함)
                localStorage.setItem('user', JSON.stringify({
                    username: user.username,
                    role: user.role
                }));
            } else {
                sessionStorage.setItem('user', JSON.stringify({
                    username: user.username,
                    role: user.role
                }));
            }
            
            // 로그인 성공 알림
            alert('로그인 성공!');
            
            // 대시보드로 이동
            window.location.href = 'dashboard.html';
        } else {
            // 로그인 실패
            alert('아이디 또는 비밀번호가 올바르지 않습니다.');
            password.value = '';
            password.focus();
        }
    });
}); 