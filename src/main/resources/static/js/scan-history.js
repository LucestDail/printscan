document.addEventListener('DOMContentLoaded', function() {
    let currentPage = 0;
    let pageSize = 10;
    let totalPages = 0;
    
    // 초기 데이터 로드
    loadScanHistory();
    loadStatistics();
    
    // 필터 변경 이벤트 리스너
    document.getElementById('filterForm').addEventListener('change', function(e) {
        if (e.target.id === 'dateRange') {
            const customRangeFields = document.querySelectorAll('#customDateRange, #customDateRange2');
            customRangeFields.forEach(field => {
                field.style.display = e.target.value === 'custom' ? 'block' : 'none';
            });
        }
        currentPage = 0;
        loadScanHistory();
        loadStatistics();
    });
    
    // 검색어 입력 이벤트 리스너 (디바운스 적용)
    const searchInput = document.getElementById('search');
    let debounceTimer;
    searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            currentPage = 0;
            loadScanHistory();
        }, 300);
    });
    
    // 스캔 이력 목록 로드
    async function loadScanHistory() {
        try {
            showLoading();
            
            const dateRange = document.getElementById('dateRange').value;
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            const department = document.getElementById('department').value;
            const search = document.getElementById('search').value;
            
            let dateFilter = '';
            if (dateRange === 'custom' && startDate && endDate) {
                dateFilter = `&startDate=${startDate}&endDate=${endDate}`;
            } else if (dateRange !== 'custom') {
                dateFilter = `&days=${dateRange}`;
            }
            
            const response = await fetch(`/api/scan/history?page=${currentPage}&size=${pageSize}&department=${department}&search=${search}${dateFilter}`);
            
            if (!response.ok) {
                throw new Error('스캔 이력을 가져오는데 실패했습니다.');
            }
            
            const data = await response.json();
            renderScanHistory(data.content);
            renderPagination(data.totalPages);
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    }
    
    // 통계 데이터 로드
    async function loadStatistics() {
        try {
            const dateRange = document.getElementById('dateRange').value;
            const startDate = document.getElementById('startDate').value;
            const endDate = document.getElementById('endDate').value;
            const department = document.getElementById('department').value;
            
            let dateFilter = '';
            if (dateRange === 'custom' && startDate && endDate) {
                dateFilter = `&startDate=${startDate}&endDate=${endDate}`;
            } else if (dateRange !== 'custom') {
                dateFilter = `&days=${dateRange}`;
            }
            
            const response = await fetch(`/api/scan/statistics?department=${department}${dateFilter}`);
            
            if (!response.ok) {
                throw new Error('통계 정보를 가져오는데 실패했습니다.');
            }
            
            const stats = await response.json();
            
            document.getElementById('totalJobs').textContent = stats.totalJobs.toLocaleString();
            document.getElementById('totalPages').textContent = stats.totalPages.toLocaleString();
            document.getElementById('colorRatio').textContent = `${stats.colorRatio}%`;
            document.getElementById('avgProcessTime').textContent = `${stats.avgProcessTime}분`;
            
        } catch (error) {
            handleError(error);
        }
    }
    
    // 스캔 이력 목록 렌더링
    function renderScanHistory(jobs) {
        const tbody = document.getElementById('historyList');
        tbody.innerHTML = '';
        
        if (jobs.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="11" class="text-center">스캔 이력이 없습니다.</td>
                </tr>
            `;
            return;
        }
        
        jobs.forEach(job => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${job.id}</td>
                <td>${job.title}</td>
                <td>${job.department}</td>
                <td>${job.requester}</td>
                <td>${job.scanOptions.colorMode === 'COLOR' ? '예' : '아니오'}</td>
                <td>${job.pageCount}</td>
                <td>${formatDate(job.requestDate)}</td>
                <td>${formatDate(job.completedDate)}</td>
                <td>${calculateProcessTime(job.requestDate, job.completedDate)}</td>
                <td>
                    <span class="badge bg-${getStatusColor(job.status)}">${job.status}</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-info" onclick="showJobDetail(${job.id})">
                        <i class="mdi mdi-eye"></i>
                    </button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }
    
    // 페이지네이션 렌더링
    function renderPagination(totalPages) {
        const pagination = document.getElementById('pagination');
        pagination.innerHTML = '';
        
        // 이전 페이지 버튼
        const prevButton = document.createElement('li');
        prevButton.className = `page-item ${currentPage === 0 ? 'disabled' : ''}`;
        prevButton.innerHTML = `
            <a class="page-link" href="#" onclick="return false;">
                <i class="mdi mdi-chevron-left"></i>
            </a>
        `;
        prevButton.addEventListener('click', () => {
            if (currentPage > 0) {
                currentPage--;
                loadScanHistory();
            }
        });
        pagination.appendChild(prevButton);
        
        // 페이지 번호
        for (let i = 0; i < totalPages; i++) {
            const pageButton = document.createElement('li');
            pageButton.className = `page-item ${currentPage === i ? 'active' : ''}`;
            pageButton.innerHTML = `
                <a class="page-link" href="#" onclick="return false;">${i + 1}</a>
            `;
            pageButton.addEventListener('click', () => {
                currentPage = i;
                loadScanHistory();
            });
            pagination.appendChild(pageButton);
        }
        
        // 다음 페이지 버튼
        const nextButton = document.createElement('li');
        nextButton.className = `page-item ${currentPage === totalPages - 1 ? 'disabled' : ''}`;
        nextButton.innerHTML = `
            <a class="page-link" href="#" onclick="return false;">
                <i class="mdi mdi-chevron-right"></i>
            </a>
        `;
        nextButton.addEventListener('click', () => {
            if (currentPage < totalPages - 1) {
                currentPage++;
                loadScanHistory();
            }
        });
        pagination.appendChild(nextButton);
    }
    
    // 작업 상세 정보 표시
    window.showJobDetail = async function(jobId) {
        try {
            showLoading();
            
            const response = await fetch(`/api/scan/jobs/${jobId}`);
            if (!response.ok) {
                throw new Error('작업 정보를 가져오는데 실패했습니다.');
            }
            
            const job = await response.json();
            
            // 모달 데이터 설정
            document.getElementById('modalTitle').textContent = job.title;
            document.getElementById('modalDepartment').textContent = job.department;
            document.getElementById('modalRequester').textContent = job.requester;
            document.getElementById('modalRequestDate').textContent = formatDate(job.requestDate);
            document.getElementById('modalCompletedDate').textContent = formatDate(job.completedDate);
            document.getElementById('modalProcessTime').textContent = calculateProcessTime(job.requestDate, job.completedDate);
            document.getElementById('modalResolution').textContent = `${job.scanOptions.resolution} DPI`;
            document.getElementById('modalColorMode').textContent = getColorModeName(job.scanOptions.colorMode);
            document.getElementById('modalFileFormat').textContent = job.scanOptions.fileFormat;
            document.getElementById('modalDoubleSided').textContent = job.scanOptions.doubleSided ? '예' : '아니오';
            document.getElementById('modalStatus').innerHTML = `<span class="badge bg-${getStatusColor(job.status)}">${job.status}</span>`;
            
            // 추가 옵션
            const options = [];
            if (job.scanOptions.autoRotate) options.push('자동 회전');
            if (job.scanOptions.autoContrast) options.push('자동 대비 조정');
            document.getElementById('modalOptions').textContent = options.join(', ') || '없음';
            
            // 스캔 파일
            const filesList = document.getElementById('modalFiles');
            if (job.files && job.files.length > 0) {
                filesList.innerHTML = job.files.map(file => `
                    <li class="list-group-item d-flex justify-content-between align-items-center">
                        <div>
                            <i class="mdi mdi-file-document"></i> ${file.name}
                            <span class="text-muted">(${formatFileSize(file.size)})</span>
                        </div>
                        <a href="/api/scan/files/${file.id}" class="btn btn-sm btn-primary">
                            <i class="mdi mdi-download"></i> 다운로드
                        </a>
                    </li>
                `).join('');
            } else {
                filesList.innerHTML = '<li class="list-group-item text-center text-muted">스캔 파일이 없습니다.</li>';
            }
            
            // 작업 로그
            const logsList = document.getElementById('modalLogs');
            if (job.logs && job.logs.length > 0) {
                logsList.innerHTML = job.logs.map(log => `
                    <li class="list-group-item">
                        <small class="text-muted">${formatDate(log.timestamp)}</small>
                        <br>${log.message}
                    </li>
                `).join('');
            } else {
                logsList.innerHTML = '<li class="list-group-item text-center text-muted">작업 로그가 없습니다.</li>';
            }
            
            // 재스캔 버튼 이벤트 리스너
            const resubmitBtn = document.getElementById('resubmitBtn');
            resubmitBtn.onclick = () => resubmitJob(job.id);
            
            // 모달 표시
            const modal = new bootstrap.Modal(document.getElementById('scanDetailModal'));
            modal.show();
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    };
    
    // 재스캔 요청
    async function resubmitJob(jobId) {
        try {
            if (!confirm('이 작업을 재스캔하시겠습니까?')) {
                return;
            }
            
            showLoading();
            
            const response = await fetch(`/api/scan/jobs/${jobId}/resubmit`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('재스캔 요청에 실패했습니다.');
            }
            
            showNotification('재스캔이 요청되었습니다.');
            
            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('scanDetailModal'));
            modal.hide();
            
            // 스캔 현황 페이지로 이동
            window.location.href = '/pages/scan/status.html';
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    }
    
    // 유틸리티 함수
    function getStatusColor(status) {
        const colors = {
            'COMPLETED': 'success',
            'ERROR': 'danger',
            'CANCELED': 'warning'
        };
        return colors[status] || 'secondary';
    }
    
    function getColorModeName(mode) {
        const names = {
            'COLOR': '컬러',
            'GRAYSCALE': '흑백(그레이스케일)',
            'BW': '흑백(이진화)'
        };
        return names[mode] || mode;
    }
    
    function calculateProcessTime(startDate, endDate) {
        if (!startDate || !endDate) return '-';
        
        const start = new Date(startDate);
        const end = new Date(endDate);
        const diff = end - start;
        
        const minutes = Math.floor(diff / 60000);
        if (minutes < 60) {
            return `${minutes}분`;
        }
        
        const hours = Math.floor(minutes / 60);
        const remainingMinutes = minutes % 60;
        return `${hours}시간 ${remainingMinutes}분`;
    }
    
    function formatDate(dateString) {
        if (!dateString) return '-';
        const date = new Date(dateString);
        return new Intl.DateTimeFormat('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        }).format(date);
    }
    
    function formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
}); 