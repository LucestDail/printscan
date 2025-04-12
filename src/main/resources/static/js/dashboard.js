document.addEventListener('DOMContentLoaded', function() {
    // 로그인 체크
    const user = JSON.parse(localStorage.getItem('user')) || JSON.parse(sessionStorage.getItem('user'));
    if (!user) {
        alert('로그인이 필요합니다.');
        window.location.href = '/login.html';
        return;
    }

    // 사용자 정보 표시
    const navbar = document.querySelector('.navbar');
    const userInfo = document.createElement('div');
    userInfo.className = 'ms-auto d-flex align-items-center';
    userInfo.innerHTML = `
        <span class="me-3">
            <i class="bx bxs-user-circle me-1"></i>
            ${user.username} (${user.role})
        </span>
    `;
    navbar.appendChild(userInfo);

    // 로그아웃 기능
    const logoutBtn = document.querySelector('.list-group-item.text-danger');
    logoutBtn.addEventListener('click', function(e) {
        e.preventDefault();
        localStorage.removeItem('user');
        sessionStorage.removeItem('user');
        window.location.href = '/login.html';
    });

    // 사이드바 토글 기능
    const menuToggle = document.getElementById('menu-toggle');
    const wrapper = document.getElementById('wrapper');
    
    menuToggle.addEventListener('click', function(e) {
        e.preventDefault();
        wrapper.querySelector('#sidebar-wrapper').classList.toggle('sidebar-expanded');
    });

    // 생산량 차트
    const productionChart = new Chart(document.getElementById('productionChart'), {
        type: 'line',
        data: {
            labels: ['1일', '2일', '3일', '4일', '5일', '6일', '7일'],
            datasets: [{
                label: '일일 생산량',
                data: [650, 720, 680, 750, 800, 720, 680],
                borderColor: '#0d6efd',
                tension: 0.1,
                fill: false
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });

    // 재고 현황 차트
    const inventoryChart = new Chart(document.getElementById('inventoryChart'), {
        type: 'bar',
        data: {
            labels: ['A4', 'A3', 'B4', 'B5', '특수지'],
            datasets: [{
                label: '현재 재고량',
                data: [1200, 800, 1500, 600, 400],
                backgroundColor: [
                    'rgba(13, 110, 253, 0.5)',
                    'rgba(25, 135, 84, 0.5)',
                    'rgba(255, 193, 7, 0.5)',
                    'rgba(220, 53, 69, 0.5)',
                    'rgba(108, 117, 125, 0.5)'
                ],
                borderColor: [
                    'rgba(13, 110, 253, 1)',
                    'rgba(25, 135, 84, 1)',
                    'rgba(255, 193, 7, 1)',
                    'rgba(220, 53, 69, 1)',
                    'rgba(108, 117, 125, 1)'
                ],
                borderWidth: 1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });
}); 