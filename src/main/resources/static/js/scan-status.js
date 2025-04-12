document.addEventListener('DOMContentLoaded', function() {
    let currentPage = 0;
    let pageSize = 10;
    let totalPages = 0;
    
    // 초기 데이터 로드
    loadScanJobs();
    
    // 필터 변경 이벤트 리스너
    document.getElementById('filterForm').addEventListener('change', function() {
        currentPage = 0;
        loadScanJobs();
    });
    
    // 검색어 입력 이벤트 리스너 (디바운스 적용)
    const searchInput = document.getElementById('search');
    let debounceTimer;
    searchInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(() => {
            currentPage = 0;
            loadScanJobs();
        }, 300);
    });
    
    // 스캔 작업 목록 로드
    async function loadScanJobs() {
        try {
            showLoading();
            
            const status = document.getElementById('status').value;
            const priority = document.getElementById('priority').value;
            const department = document.getElementById('department').value;
            const search = document.getElementById('search').value;
            
            const response = await fetch(`/api/scan/jobs?page=${currentPage}&size=${pageSize}&status=${status}&priority=${priority}&department=${department}&search=${search}`);
            
            if (!response.ok) {
                throw new Error('스캔 작업 목록을 가져오는데 실패했습니다.');
            }
            
            const data = await response.json();
            renderScanJobs(data.content);
            renderPagination(data.totalPages);
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    }
    
    // 스캔 작업 목록 렌더링
    function renderScanJobs(jobs) {
        const tbody = document.getElementById('scanList');
        tbody.innerHTML = '';
        
        if (jobs.length === 0) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="9" class="text-center">스캔 작업이 없습니다.</td>
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
                <td>
                    <span class="badge bg-${getPriorityColor(job.priority)}">${job.priority}</span>
                </td>
                <td>
                    <span class="badge bg-${getStatusColor(job.status)}">${job.status}</span>
                </td>
                <td>${formatDate(job.requestDate)}</td>
                <td>${formatDate(job.completedDate)}</td>
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
                loadScanJobs();
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
                loadScanJobs();
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
                loadScanJobs();
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
            
            // 작업 취소 버튼 상태 설정
            const cancelBtn = document.getElementById('cancelBtn');
            cancelBtn.style.display = job.status === 'WAITING' || job.status === 'IN_PROGRESS' ? 'block' : 'none';
            cancelBtn.onclick = () => cancelJob(job.id);
            
            // 모달 표시
            const modal = new bootstrap.Modal(document.getElementById('scanDetailModal'));
            modal.show();
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    };
    
    // 작업 취소
    async function cancelJob(jobId) {
        try {
            if (!confirm('이 작업을 취소하시겠습니까?')) {
                return;
            }
            
            showLoading();
            
            const response = await fetch(`/api/scan/jobs/${jobId}/cancel`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('작업 취소에 실패했습니다.');
            }
            
            showNotification('작업이 취소되었습니다.');
            
            // 모달 닫기
            const modal = bootstrap.Modal.getInstance(document.getElementById('scanDetailModal'));
            modal.hide();
            
            // 목록 새로고침
            loadScanJobs();
            
        } catch (error) {
            handleError(error);
        } finally {
            hideLoading();
        }
    }
    
    // 유틸리티 함수
    function getStatusColor(status) {
        const colors = {
            'WAITING': 'secondary',
            'IN_PROGRESS': 'info',
            'COMPLETED': 'success',
            'ERROR': 'danger',
            'CANCELED': 'warning'
        };
        return colors[status] || 'secondary';
    }
    
    function getPriorityColor(priority) {
        const colors = {
            'LOW': 'secondary',
            'MEDIUM': 'info',
            'HIGH': 'warning',
            'URGENT': 'danger'
        };
        return colors[priority] || 'secondary';
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