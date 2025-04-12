document.addEventListener('DOMContentLoaded', function() {
    // 테스트 데이터를 가져오는 함수
    async function fetchTestData() {
        try {
            const response = await fetch('/api/test');
            const data = await response.json();
            
            const testResult = document.querySelector('.test-result');
            if (data) {
                testResult.innerHTML = `
                    <div class="alert alert-success">
                        <h4>데이터베이스 연결 성공!</h4>
                        <pre class="mt-3">${JSON.stringify(data, null, 2)}</pre>
                    </div>
                `;
            }
        } catch (error) {
            const testResult = document.querySelector('.test-result');
            testResult.innerHTML = `
                <div class="alert alert-danger">
                    <h4>데이터베이스 연결 실패</h4>
                    <p>${error.message}</p>
                </div>
            `;
        }
    }

    // 페이지 로드시 테스트 데이터 가져오기
    fetchTestData();

    // 회사 정보 가져오기
    async function fetchCompanyInfo() {
        try {
            const response = await fetch('/api/company/info', {
                headers: {
                    'Accept': 'application/json;charset=UTF-8',
                    'Content-Type': 'application/json;charset=UTF-8'
                }
            });
            const text = await response.text(); // 먼저 텍스트로 받음
            const data = JSON.parse(text); // 텍스트를 JSON으로 파싱
            
            // UTF-8 디코딩을 명시적으로 처리
            const decoder = new TextDecoder('utf-8');
            document.getElementById('pageTitle').textContent = decoder.decode(new TextEncoder().encode(data.systemName));
            document.getElementById('companyName').textContent = decoder.decode(new TextEncoder().encode(data.systemName));
        } catch (error) {
            console.error('회사 정보를 가져오는데 실패했습니다:', error);
        }
    }

    // 페이지 로드시 회사 정보 가져오기
    fetchCompanyInfo();

    // 사이드바 토글
    document.getElementById('sidebarCollapse').addEventListener('click', function() {
        document.getElementById('sidebar').classList.toggle('active');
    });

    // 작업 처리 현황 차트
    const ctx = document.getElementById('workChart').getContext('2d');
    new Chart(ctx, {
        type: 'line',
        data: {
            labels: ['1월', '2월', '3월', '4월', '5월', '6월'],
            datasets: [{
                label: '인쇄 작업',
                data: [65, 59, 80, 81, 56, 55],
                borderColor: '#2196F3',
                tension: 0.1
            }, {
                label: '스캔 작업',
                data: [28, 48, 40, 19, 86, 27],
                borderColor: '#4CAF50',
                tension: 0.1
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                },
                title: {
                    display: true,
                    text: '월별 작업 처리 현황'
                }
            },
            scales: {
                y: {
                    beginAtZero: true
                }
            }
        }
    });

    // 알림 갱신
    function updateNotifications() {
        fetch('/api/notifications')
            .then(response => response.json())
            .then(data => {
                // 알림 업데이트 로직
            })
            .catch(error => console.error('Error:', error));
    }

    // 실시간 데이터 갱신
    function updateDashboardData() {
        fetch('/api/dashboard/stats')
            .then(response => response.json())
            .then(data => {
                // 대시보드 데이터 업데이트 로직
            })
            .catch(error => console.error('Error:', error));
    }

    // 주기적 데이터 갱신
    setInterval(updateNotifications, 60000); // 1분마다
    setInterval(updateDashboardData, 300000); // 5분마다
}); 