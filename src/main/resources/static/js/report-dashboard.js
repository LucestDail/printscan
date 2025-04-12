document.addEventListener('DOMContentLoaded', function() {
    let currentPeriod = 'daily';
    let jobTrendChart = null;
    let departmentUsageChart = null;

    // 기간 선택 버튼 이벤트
    document.getElementById('btnDaily').addEventListener('click', () => changePeriod('daily'));
    document.getElementById('btnWeekly').addEventListener('click', () => changePeriod('weekly'));
    document.getElementById('btnMonthly').addEventListener('click', () => changePeriod('monthly'));

    // 초기 데이터 로드
    changePeriod('daily');

    // 기간 변경 처리
    function changePeriod(period) {
        currentPeriod = period;
        updateButtonStates(period);
        loadDashboardData(period);
    }

    // 버튼 상태 업데이트
    function updateButtonStates(period) {
        const buttons = {
            'daily': document.getElementById('btnDaily'),
            'weekly': document.getElementById('btnWeekly'),
            'monthly': document.getElementById('btnMonthly')
        };

        Object.keys(buttons).forEach(key => {
            if (key === period) {
                buttons[key].classList.remove('btn-outline-primary');
                buttons[key].classList.add('btn-primary');
            } else {
                buttons[key].classList.remove('btn-primary');
                buttons[key].classList.add('btn-outline-primary');
            }
        });
    }

    // 대시보드 데이터 로드
    async function loadDashboardData(period) {
        try {
            const response = await fetch(`/api/reports/dashboard?period=${period}`);
            if (!response.ok) throw new Error('대시보드 데이터를 불러오는데 실패했습니다.');
            
            const data = await response.json();
            updateDashboard(data);

        } catch (error) {
            console.error('대시보드 데이터 로드 오류:', error);
            showNotification('대시보드 데이터를 불러오는데 실패했습니다.', 'error');
        }
    }

    // 대시보드 업데이트
    function updateDashboard(data) {
        // 통계 카드 업데이트
        document.getElementById('printJobCount').textContent = data.printJobCount.toLocaleString();
        document.getElementById('scanJobCount').textContent = data.scanJobCount.toLocaleString();
        document.getElementById('totalPages').textContent = data.totalPages.toLocaleString();
        document.getElementById('activeUsers').textContent = data.activeUsers.toLocaleString();

        // 인쇄 작업 상세 업데이트
        document.getElementById('colorPrintRatio').textContent = `${data.colorPrintRatio}%`;
        document.getElementById('duplexPrintRatio').textContent = `${data.duplexPrintRatio}%`;
        document.getElementById('avgPrintTime').textContent = `${data.avgPrintTime}분`;
        document.getElementById('printFailureRate').textContent = `${data.printFailureRate}%`;

        // 스캔 작업 상세 업데이트
        document.getElementById('colorScanRatio').textContent = `${data.colorScanRatio}%`;
        document.getElementById('ocrUsageRatio').textContent = `${data.ocrUsageRatio}%`;
        document.getElementById('avgScanTime').textContent = `${data.avgScanTime}분`;
        document.getElementById('scanFailureRate').textContent = `${data.scanFailureRate}%`;

        // 차트 업데이트
        updateJobTrendChart(data.jobTrend);
        updateDepartmentUsageChart(data.departmentUsage);
    }

    // 작업 추이 차트 업데이트
    function updateJobTrendChart(data) {
        const ctx = document.getElementById('jobTrendChart').getContext('2d');
        
        if (jobTrendChart) {
            jobTrendChart.destroy();
        }

        jobTrendChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: data.labels,
                datasets: [
                    {
                        label: '인쇄 작업',
                        data: data.printJobs,
                        borderColor: '#0d6efd',
                        backgroundColor: 'rgba(13, 110, 253, 0.1)',
                        tension: 0.4,
                        fill: true
                    },
                    {
                        label: '스캔 작업',
                        data: data.scanJobs,
                        borderColor: '#198754',
                        backgroundColor: 'rgba(25, 135, 84, 0.1)',
                        tension: 0.4,
                        fill: true
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'top'
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: {
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    // 부서별 사용량 차트 업데이트
    function updateDepartmentUsageChart(data) {
        const ctx = document.getElementById('departmentUsageChart').getContext('2d');
        
        if (departmentUsageChart) {
            departmentUsageChart.destroy();
        }

        departmentUsageChart = new Chart(ctx, {
            type: 'doughnut',
            data: {
                labels: data.departments,
                datasets: [{
                    data: data.usage,
                    backgroundColor: [
                        '#0d6efd',
                        '#198754',
                        '#ffc107',
                        '#dc3545',
                        '#6610f2',
                        '#fd7e14',
                        '#20c997',
                        '#0dcaf0'
                    ]
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom'
                    }
                }
            }
        });
    }

    // 날짜 포맷
    function formatDate(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('ko-KR', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    }
}); 